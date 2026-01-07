package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Trade;
import com.example.heartbit.dto.TradeRequest;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.TradeRepository;


import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final CategoryRepository categoryRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;
    private final AssetService assetService;

    // 종목별 실시간 시세 상태 관리 (메모리 맵)
    private final Map<Long, BigDecimal> openPrices = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> currentPrices = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> dailyHighs = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> dailyLows = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> accVolumes = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> accAmounts = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> totalBuyQtys = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> totalSellQtys = new ConcurrentHashMap<>();

    // 종목별 차트용 변수 (메모리 맵)
    private final Map<Long, BigDecimal> candleOpens = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> candleHighs = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> candleLows = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> currentMinutes = new ConcurrentHashMap<>();

    /**
     * 서버 재시작 시 오늘 오전 9시 이후의 시세 데이터를 DB에서 복구합니다.
     */
    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today9AM = now.withHour(9).withMinute(0).withSecond(0).withNano(0);
        if (now.isBefore(today9AM)) today9AM = today9AM.minusDays(1);

        List<Category> categories = categoryRepository.findAll();

        for (Category category : categories) {
            Long id = category.getCategoryId();

            // 1. 기준가 및 현재가 로드
            tradeRepository.findTop1ByTradeTimeBeforeOrderByTradeTimeDesc(today9AM)
                    .ifPresent(t -> openPrices.put(id, t.getTradePrice()));

            tradeRepository.findTop1ByCategoryOrderByTradeTimeDesc(id)
                    .ifPresent(t -> {
                        BigDecimal price = t.getTradePrice();
                        currentPrices.put(id, price);
                        candleOpens.put(id, price);
                        candleHighs.put(id, price);
                        candleLows.put(id, price);
                        currentMinutes.put(id, t.getTradeTime().withSecond(0).withNano(0));
                    });

            // 2. [개선] 집계 데이터를 한 번에 가져오기 (TradeRepository에 집계 쿼리 구현 권장)
            // 여기서는 리스트를 쓰신다면 최소한 거래대금과 체결강도용 수량도 복구해야 합니다.
            List<Trade> todayTrades = tradeRepository.findTradesByCategoryIdAndTradeTimeAfter(id, today9AM);
            if (!todayTrades.isEmpty()) {
                dailyHighs.put(id, todayTrades.stream().map(Trade::getTradePrice).max(BigDecimal::compareTo).get());
                dailyLows.put(id, todayTrades.stream().map(Trade::getTradePrice).min(BigDecimal::compareTo).get());

                // 거래량 및 거래대금 복구
                accVolumes.put(id, todayTrades.stream().map(Trade::getTradeCount).reduce(BigDecimal.ZERO, BigDecimal::add));
                accAmounts.put(id, todayTrades.stream().map(t -> t.getTradePrice().multiply(t.getTradeCount())).reduce(BigDecimal.ZERO, BigDecimal::add));

                // 체결강도용 매수/매도 누적량 복구 (TakerType 기준)
                // Note: 실제 DB에 TakerType이 저장되어 있어야 정확합니다.
                // totalBuyQtys.put(id, ...);
                // totalSellQtys.put(id, ...);
            }
        }
    }

    /**
     * [체결 처리] 엔진의 체결 결과 리스트를 순회하며 DB 저장 및 시세를 업데이트합니다.
     */
    @Transactional
    @Operation(summary = "체결 엔진 결과 처리", description = "체결 데이터를 저장하고 매도자에게 대금을 정산합니다.")
    public void processTradeResults(Long categoryId, List<TradeResponse> tradeResults) {
        if (tradeResults.isEmpty()) return;

        for (TradeResponse response : tradeResults) {
            // 1. 주문 정보 상세 조회 (자산 처리를 위해 실제 객체 필요)
            com.example.heartbit.domain.Order sellOrder = orderRepository.findById(response.getSellOrderId())
                    .orElseThrow(() -> new NoSuchElementException("매도 주문을 찾을 수 없습니다."));

            // 2. [핵심] 자산 정산: 매도자에게 체결 대금 지급
            BigDecimal tradeAmount = response.getTradePrice().multiply(response.getTradeCount());

            // 관리자 계정(1L)이 아닌 경우에만 실제 돈을 지급 (유동성 공급용 계정 제외 로직)
            if (!sellOrder.getMember().getMemberId().equals(1L)) {
                // 매도 완료 후 현금(Cash)으로 정산
                assetService.refundCash(sellOrder.getMember().getMemberId(), tradeAmount);
            }

            // 3. Trade 엔티티 생성 및 저장
            Trade trade = Trade.builder()
                    .tradePrice(response.getTradePrice())
                    .tradeCount(response.getTradeCount())
                    .buyOrder(orderRepository.getReferenceById(response.getBuyOrderId()))
                    .sellOrder(sellOrder) // 위에서 찾은 sellOrder 활용
                    .tradeTime(response.getTradeTime())
                    .build();
            tradeRepository.save(trade);

            // 4. 종목별 상태 업데이트 및 웹소켓 전송
            updateMarketAndBroadcast(categoryId, response);
        }
    }

    private void updateMarketAndBroadcast(Long categoryId, TradeResponse response) {
        BigDecimal price = response.getTradePrice();
        BigDecimal count = response.getTradeCount();

        // 실시간 맵 데이터 갱신
        currentPrices.put(categoryId, price);
        dailyHighs.merge(categoryId, price, (old, val) -> val.compareTo(old) > 0 ? val : old);
        dailyLows.merge(categoryId, price, (old, val) -> val.compareTo(old) < 0 ? val : old);
        accVolumes.merge(categoryId, count, BigDecimal::add);
        accAmounts.merge(categoryId, price.multiply(count), BigDecimal::add);

        // 체결강도 계산용 수량 업데이트
        if ("BUY".equals(response.getTakerType())) totalBuyQtys.merge(categoryId, count, BigDecimal::add);
        else totalSellQtys.merge(categoryId, count, BigDecimal::add);

        updateCandle(categoryId, price, response.getTradeTime());
        sendWebSocketData(categoryId, response);
    }

    private void sendWebSocketData(Long categoryId, TradeResponse response) {
        String suffix = "/" + categoryId;
        BigDecimal price = response.getTradePrice();

        BigDecimal buyQty = totalBuyQtys.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal sellQty = totalSellQtys.getOrDefault(categoryId, BigDecimal.ONE); // 분모 0 방지

        // 계산식: (매수체결량 / 매도체결량) * 100
        BigDecimal intensity = buyQty.divide(sellQty, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        BigDecimal openPrice = openPrices.getOrDefault(categoryId, price);

        // 변동률 및 시세 데이터 조립
        BigDecimal changeAmount = price.subtract(openPrice);
        BigDecimal changeRate = openPrice.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                changeAmount.divide(openPrice, 10, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

        Map<String, Object> ticker = new HashMap<>();
        ticker.put("price", price.toPlainString());
        ticker.put("changeAmount", changeAmount.toPlainString());
        ticker.put("changeRate", changeRate.setScale(2, RoundingMode.HALF_UP).toPlainString());
        ticker.put("high", dailyHighs.getOrDefault(categoryId, price).toPlainString());
        ticker.put("low", dailyLows.getOrDefault(categoryId, price).toPlainString());
        ticker.put("volume", accVolumes.getOrDefault(categoryId, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString());
        ticker.put("amount", accAmounts.getOrDefault(categoryId, BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP).toPlainString());

        messagingTemplate.convertAndSend("/topic/ticker" + suffix, (Object)ticker);

        Map<String, Object> trades = new HashMap<>();
        trades.put("price", price.toPlainString());
        trades.put("count", response.getTradeCount().toPlainString());
        trades.put("type", response.getTakerType());
        trades.put("time", response.getTradeTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        trades.put("intensity", intensity.setScale(2, RoundingMode.HALF_UP).toPlainString());
        messagingTemplate.convertAndSend("/topic/trades" + suffix, (Object)trades);

        Map<String, Object> candle = new HashMap<>();
        candle.put("t", currentMinutes.get(categoryId).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        candle.put("o", candleOpens.get(categoryId).toPlainString());
        candle.put("h", candleHighs.get(categoryId).toPlainString());
        candle.put("l", candleLows.get(categoryId).toPlainString());
        candle.put("c", price.toPlainString());
        messagingTemplate.convertAndSend("/topic/charts" + suffix, (Object)candle);

        messagingTemplate.convertAndSend("/topic/orderbook/lastPrice/" + categoryId, price.toPlainString());
    }

    // --- [조회 API 기능들] ---

    public List<TradeResponse> getTradeList(Long categoryId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return tradeRepository.findTopTradesByCategory(categoryId, pageable).stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public TradeResponse getRecentTrade(Long categoryId) {
        return tradeRepository.findTop1ByCategoryOrderByTradeTimeDesc(categoryId)
                .map(TradeResponse::fromEntity)
                .orElse(null);
    }

    public List<TradeResponse> getTradeByOrder(Long orderId) {
        return tradeRepository.findByBuyOrder_OrderIdOrSellOrder_OrderId(orderId, orderId).stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TradeResponse> getMyTrade(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return tradeRepository.findTradeByMemberId(memberId, pageable).getContent().stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getInitialCandles(Long categoryId) {
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
        // 조인 쿼리를 통한 종목별 데이터 조회
        List<Trade> trades = tradeRepository.findTradesByCategoryIdAndTradeTimeAfter(categoryId, fifteenMinutesAgo);

        if (trades.isEmpty()) return Collections.emptyList();

        Map<LocalDateTime, List<Trade>> groupedTrades = trades.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTradeTime().withSecond(0).withNano(0),
                        TreeMap::new,
                        Collectors.toList()
                ));

        return groupedTrades.entrySet().stream().map(entry -> {
            List<Trade> minuteTrades = entry.getValue();
            Map<String, Object> candle = new HashMap<>();
            candle.put("t", entry.getKey().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            candle.put("o", minuteTrades.get(0).getTradePrice().toPlainString());
            candle.put("h", minuteTrades.stream().map(Trade::getTradePrice).max(BigDecimal::compareTo).get().toPlainString());
            candle.put("l", minuteTrades.stream().map(Trade::getTradePrice).min(BigDecimal::compareTo).get().toPlainString());
            candle.put("c", minuteTrades.get(minuteTrades.size() - 1).getTradePrice().toPlainString());
            return candle;
        }).collect(Collectors.toList());
    }

    private void updateCandle(Long categoryId, BigDecimal price, LocalDateTime tradeTime) {
        LocalDateTime nowMinute = tradeTime.withSecond(0).withNano(0);
        LocalDateTime currentMinute = currentMinutes.getOrDefault(categoryId, LocalDateTime.MIN);

        if (nowMinute.isAfter(currentMinute)) {
            candleOpens.put(categoryId, price);
            candleHighs.put(categoryId, price);
            candleLows.put(categoryId, price);
            currentMinutes.put(categoryId, nowMinute);
        } else {
            candleHighs.merge(categoryId, price, (old, val) -> val.compareTo(old) > 0 ? val : old);
            candleLows.merge(categoryId, price, (old, val) -> val.compareTo(old) < 0 ? val : old);
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void refreshMarket() {
        openPrices.putAll(currentPrices);
        accVolumes.clear();
        accAmounts.clear();
        log.info("오전 9시 장 개시 - 기준가 갱신 완료");
    }
}