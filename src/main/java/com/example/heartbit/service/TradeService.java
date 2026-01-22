package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.Trade;
import com.example.heartbit.dto.CategoryDto;
import com.example.heartbit.dto.PriceChangedEvent;
import com.example.heartbit.dto.TradeRequest;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.web.PageableArgumentResolver;
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

import static com.example.heartbit.util.RedisKeyUtils.getTickerKey;


@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final InvestService investService;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;
    private final AssetService assetService;

    private final StringRedisTemplate redisTemplate;


    // 종목별 실시간 시세 상태 관리 (메모리 맵)
    private final Map<Long, BigDecimal> openPrices = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> currentPrices = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> changeAmounts = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> changeRates = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> dailyHighs = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> dailyLows = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> accVolumes = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> accAmounts = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> totalBuyQtys = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> totalSellQtys = new ConcurrentHashMap<>();
    private final Map<Long, String> takerType = new ConcurrentHashMap<>();

    // 종목별 차트용 변수 (메모리 맵)
    private final Map<Long, BigDecimal> candleOpens = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> candleHighs = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> candleLows = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> currentMinutes = new ConcurrentHashMap<>();
    private final PageableArgumentResolver pageableArgumentResolver;

    /**
     * 서버 재시작 시 오늘 오전 9시 이후의 시세 데이터를 DB에서 복구
     */
    @Transactional(readOnly = true)
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today9AM = now.withHour(9).withMinute(0).withSecond(0).withNano(0);
        if (now.isBefore(today9AM)) today9AM = today9AM.minusDays(1);

        List<Category> categories = categoryRepository.findAll();

        for (Category category : categories) {
            Long id = category.getCategoryId();

            // 기준가 및 현재가 로드(오전 9시 이전의 체결이 있다면)
            tradeRepository.findTop1ByCategoryIdAndTradeTimeBefore(id, today9AM)
                    .ifPresent(t -> {
                        openPrices.put(id, t.getTradePrice());
                    });

            //종목별로 최근 체결 내역 1건 가져옴
            tradeRepository.findTop1ByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(id)
                    .ifPresent(t -> {
                        BigDecimal price = t.getTradePrice();
                        currentPrices.put(id, price);

                        String type = t.getBuyOrder().getOrderTime().isAfter(t.getSellOrder().getOrderTime()) ? "BUY" : "SELL";
                        takerType.put(id, type);
                        candleOpens.put(id, price);
                        candleHighs.put(id, price);
                        candleLows.put(id, price);
                        currentMinutes.put(id, t.getTradeTime().withSecond(0).withNano(0));
                    });


            // 여기서는 리스트를 쓰면 최소한 거래대금과 체결강도용 수량도 복구
            List<Trade> todayTrades = tradeRepository.findTradesByCategoryIdAndTradeTimeAfter(id, today9AM);
            if (!todayTrades.isEmpty()) {
                dailyHighs.put(id, todayTrades.stream().map(Trade::getTradePrice).max(BigDecimal::compareTo).get());
                dailyLows.put(id, todayTrades.stream().map(Trade::getTradePrice).min(BigDecimal::compareTo).get());

                // 거래량 및 거래대금 복구
                accVolumes.put(id, todayTrades.stream().map(Trade::getTradeCount).reduce(BigDecimal.ZERO, BigDecimal::add));
                accAmounts.put(id, todayTrades.stream().map(t -> t.getTradePrice().multiply(t.getTradeCount())).reduce(BigDecimal.ZERO, BigDecimal::add));


                //총 매수 거래량
                BigDecimal buyVol = todayTrades.stream()
                        .filter(t -> t.getBuyOrder().getOrderTime().isAfter(t.getSellOrder().getOrderTime())) // 매수자가 늦게 주문했으면 매수 체결(BUY Taker)
                        .map(Trade::getTradeCount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                //총 매도 거래량
                BigDecimal sellVol = todayTrades.stream()
                        .filter(t -> t.getSellOrder().getOrderTime().isAfter(t.getBuyOrder().getOrderTime())) // 매도자가 늦게 주문했으면 매도 체결(SELL Taker)
                        .map(Trade::getTradeCount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                totalBuyQtys.put(id, buyVol);
                totalSellQtys.put(id, sellVol);

                BigDecimal currentPrice = currentPrices.getOrDefault(id, BigDecimal.ZERO);
                BigDecimal openPrice = openPrices.getOrDefault(id, BigDecimal.ZERO);

                // 만약 오전 9시 이전 데이터가 없어서 시가가 0인데, 현재가는 있는 경우 (신규 상장 or 데이터 유실 등)
                // 시가를 현재가로 맞춰주어 변동률을 0%로 시작하게 함 (방어 로직)
                if (openPrice.compareTo(BigDecimal.ZERO) == 0 && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                    openPrice = currentPrice;
                    openPrices.put(id, openPrice);
                }

                // 시가가 존재할 때만 계산 수행
                if (openPrice.compareTo(BigDecimal.ZERO) > 0) {
                    // 변동금 = 현재가 - 시가
                    BigDecimal changeAmount = currentPrice.subtract(openPrice);

                    // 변동률 = (변동금 / 시가) * 100
                    BigDecimal changeRate = changeAmount.divide(openPrice, 10, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));

                    // **여기서 맵에 넣어줘야 REST API 호출 시 0이 안 나옴**
                    changeAmounts.put(id, changeAmount);
                    changeRates.put(id, changeRate);
                }


            }
        }
    }

    /**
     * 체결 엔진의 체결 결과 리스트를 순회하며 DB 저장 및 시세를 업데이트
     */
    @Transactional
    @Operation(summary = "체결 엔진 결과 처리", description = "체결 데이터를 저장하고 매도자에게 대금을 정산합니다.")
    public void processTradeResults(Long categoryId, List<TradeResponse> tradeResults) {
        if (tradeResults.isEmpty()) return;

        for (TradeResponse response : tradeResults) {
            Order buyOrder = orderRepository.findById(response.getBuyOrderId())
                    .orElseThrow(() -> new NoSuchElementException("매수 주문을 찾을 수 없습니다."));
            // 주문 정보 상세 조회 (자산 처리를 위해 실제 객체 필요)
            Order sellOrder = orderRepository.findById(response.getSellOrderId())
                    .orElseThrow(() -> new NoSuchElementException("매도 주문을 찾을 수 없습니다."));

            // 주문 수량 변경 값 db 저장
            BigDecimal tradeAmount = response.getTradePrice().multiply(response.getTradeCount());

            // 관리자 계정(5L)이 아닌 경우에만 실제 돈을 지급 (유동성 공급용 계정 제외 로직)
            if (!sellOrder.getMember().getMemberId().equals(1L)) {
                // 매도 완료 후 현금(Cash)으로 정산
                assetService.refundCash(sellOrder.getMember().getMemberId(), tradeAmount);
            }




            Trade trade = Trade.builder()
                    .tradePrice(response.getTradePrice())
                    .tradeCount(response.getTradeCount())
                    .tradeClosePrice(response.getTradeClosePrice())
                    .buyOrder(buyOrder)
                    .sellOrder(sellOrder) // 위에서 찾은 sellOrder 활용
                    .tradeTime(response.getTradeTime())
                    .build();

            // trade 값 저장
            Trade savedTrade = tradeRepository.save(trade);

            investService.saveOrUpdateInvest(
                    buyOrder.getMember().getMemberId(),
                    savedTrade,    // [수정] Trade 객체 전달
                    categoryId,
                    response.getTradeCount(),
                    response.getTradePrice(),
                    "BUY"
            );

            // 3-2. 매도자 자산 업데이트 ("SELL")
            if (!sellOrder.getMember().getMemberId().equals(1L)) {
                investService.saveOrUpdateInvest(
                        sellOrder.getMember().getMemberId(),
                        savedTrade,    // [수정] Trade 객체 전달
                        categoryId,
                        response.getTradeCount(),
                        response.getTradePrice(),
                        "SELL"
                );
            }
            eventPublisher.publishEvent(new PriceChangedEvent(categoryId, response.getTradePrice()));
            //종목별 상태 업데이트 및 웹소켓 전송
            updateMarketAndBroadcast(categoryId, response);
        }
    }

    //값이 바뀌면 값들 갱신하고 웹소켓으로 쏴주는 매서드 호출
    private void updateMarketAndBroadcast(Long categoryId, TradeResponse response) {
        BigDecimal price = response.getTradePrice();
        String key = getTickerKey(categoryId);

        // Redis에 현재가 저장 (String 타입)
        redisTemplate.opsForValue().set(key, price.toPlainString());
        BigDecimal count = response.getTradeCount();
        BigDecimal openPrice = openPrices.getOrDefault(categoryId, price);

        if (openPrice.compareTo(BigDecimal.ZERO) <= 0) {
            openPrice = price;
            openPrices.put(categoryId, price);
        }

        BigDecimal changeAmount = price.subtract(openPrice);
        BigDecimal changeRate = changeAmount.divide(openPrice, 10, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        // 실시간 맵 데이터 갱신
        takerType.put(categoryId, response.getTakerType());
        currentPrices.put(categoryId, price);
        changeAmounts.put(categoryId, changeAmount);
        changeRates.put(categoryId, changeRate);
        dailyHighs.merge(categoryId, price, (old, val) -> val.compareTo(old) > 0 ? val : old);
        dailyLows.merge(categoryId, price, (old, val) -> val.compareTo(old) < 0 ? val : old);
        accVolumes.merge(categoryId, count, BigDecimal::add);
        accAmounts.merge(categoryId, price.multiply(count), BigDecimal::add);

        // 체결강도 계산용 수량 업데이트
        if ("BUY".equals(response.getTakerType())) totalBuyQtys.merge(categoryId, count, BigDecimal::add);
        else totalSellQtys.merge(categoryId, count, BigDecimal::add);

        eventPublisher.publishEvent(new PriceChangedEvent(categoryId, price));

        updateCandle(categoryId, price, response.getTradeTime());
        sendWebSocketData(categoryId, response);
    }

    //분단위로 계산하여 nowMinute이 currentMinute보다 커지게 되면 캔틀 하나 옆으로 이동(그게 아니라면 캔들은 제자리에서 위아래로만)
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


    //실시간으로 웹소켓으로 데이터 전송
    private void sendWebSocketData(Long categoryId, TradeResponse response) {
        String suffix = "/" + categoryId;
        BigDecimal price = response.getTradePrice();

        BigDecimal buyQty = totalBuyQtys.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal sellQty = totalSellQtys.getOrDefault(categoryId, BigDecimal.ONE);

        if (sellQty.compareTo(BigDecimal.ZERO) == 0) {
            sellQty = BigDecimal.ONE;
        }// 분모 0 방지

        // 체결강도 계산식: (매수체결량 / 매도체결량) * 100
        BigDecimal intensity = buyQty.divide(sellQty, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        BigDecimal openPrice = openPrices.getOrDefault(categoryId, price);

        // 변동률 및 시세 데이터
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
        trades.put("openPrice", openPrice.toPlainString());
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



    //최근 체결 기록(limit으로 개수 설정 가능)
    public List<TradeResponse> getTradeList(Long categoryId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return tradeRepository.findByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(categoryId, pageable)
                .stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    //해당 종목의 현재가 1개 가져오기
    public TradeResponse getRecentTrade(Long categoryId) {
        return tradeRepository.findTop1ByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(categoryId)
                .map(TradeResponse::fromEntity)
                .orElse(null);
    }


    //주문별로 실제로 얼마나 체결되었는지 확인하기
    public List<TradeResponse> getTradeByOrder(Long orderId) {
        // 먼저 주문 정보를 가져와서 작성자 누군지 확인
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다."));
        Long memberId = order.getMember().getMemberId();

        // 해당 주문과 연결된 체결 내역을 가져온 뒤, 사용자 ID를 넘겨서 타입을 결정
        return tradeRepository.findByBuyOrder_OrderIdOrSellOrder_OrderId(orderId, orderId).stream()
                .map(t -> TradeResponse.fromEntityWithOrderType(t, memberId)) // 사용자 ID 전달
                .collect(Collectors.toList());
    }

    //개인 체결 내역
    public List<TradeResponse> getMyTrade(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return tradeRepository.findTradeByMemberId(memberId, pageable)
                .getContent()
                .stream()
                // 각 체결건(t)에 대해 조회 주체(memberId)를 기준으로 BUY/SELL을 결정
                .map(t -> TradeResponse.fromEntityWithOrderType(t, memberId))
                .collect(Collectors.toList());
    }


    //사용자가 처음에 접속했을때 텅빈 화면이 뜨는것을 방지하기 위해 db에서 지난 차트 데이터들을 REST API로 불러오기
    public List<Map<String, Object>> getInitialCandles(Long categoryId, Long lastId, int size) {

        Pageable pageable = PageRequest.of(0, size);
        List<Trade> trades;

        if(lastId == null || lastId == 0) {
            trades = tradeRepository.findLatestTrades(categoryId, pageable);
        } else {
            trades = tradeRepository.findTradesByCursor(categoryId, lastId, pageable);
        }

        if (trades.isEmpty()) return Collections.emptyList();

        //1분봉으로 그룹핑
        Map<LocalDateTime, List<Trade>> groupedTrades = trades.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTradeTime().withSecond(0).withNano(0),
                        () -> new TreeMap<LocalDateTime, List<Trade>>(Comparator.reverseOrder()),
                        Collectors.toList()
                ));

        return groupedTrades.entrySet().stream().map(entry -> {
            LocalDateTime minute = entry.getKey();
            List<Trade> minuteTrades = entry.getValue();

            // DB에서 Desc로 가져왔기 때문에 리스트의 끝[size-1]이 그 분의 첫 거래
            BigDecimal open = minuteTrades.get(minuteTrades.size() - 1).getTradePrice(); // 시가
            BigDecimal close = minuteTrades.get(0).getTradePrice(); // 종가

            BigDecimal high = minuteTrades.stream()
                    .map(Trade::getTradePrice)
                    .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal low = minuteTrades.stream()
                    .map(Trade::getTradePrice)
                    .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            Long minTradeIdInCandle = minuteTrades.stream()
                    .map(Trade::getTradeId)
                    .min(Long::compareTo).orElse(0L);

            Map<String, Object> candle = new HashMap<>();
            candle.put("t", minute.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            candle.put("o", open.toPlainString());
            candle.put("h", high.toPlainString());
            candle.put("l", low.toPlainString());
            candle.put("c", close.toPlainString());
            candle.put("tradeId", minTradeIdInCandle);

            return candle;
        }).collect(Collectors.toList());
    }



    //오전 9시되면 장 초기화
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void refreshMarket() {
        for (Long id : currentPrices.keySet()) {
            BigDecimal closePrice = currentPrices.get(id);
            openPrices.put(id, closePrice);

            // 고가/저가를 현재가로 초기화 (0으로 하면 비교 로직이 꼬임)
            dailyHighs.put(id, closePrice);
            dailyLows.put(id, closePrice);
        }

        // 누적 데이터 전량 삭제
        accVolumes.clear();
        accAmounts.clear();
        totalBuyQtys.clear();
        totalSellQtys.clear();
    }

    /**
     * 특정 종목(categoryId)의 실시간 현재가 정보를 반환합니다.
     */
    public TradeResponse getCurrentTrade(Long categoryId) {
        String key = getTickerKey(categoryId);

        // 1. Redis에서 가격 조회
        Object cachedPrice = redisTemplate.opsForValue().get(key);
        BigDecimal price;

        if (cachedPrice != null) {
            price = new BigDecimal(cachedPrice.toString());
        } else {
            // 2. Redis에 없으면 DB에서 최신 체결가 가져오기 (방어 로직)
            TradeResponse recent = getRecentTrade(categoryId);
            price = (recent != null) ? recent.getTradePrice() : BigDecimal.ZERO;

            // 3. DB에서 가져온 값을 다시 Redis에 캐싱 (다음 조회를 위해)
            if (price.compareTo(BigDecimal.ZERO) > 0) {
                redisTemplate.opsForValue().set(key, price.toPlainString());
            }
        }

        return TradeResponse.builder().tradePrice(price).build();
    }
    /**
     * 종목 단건 조회 (투자용)
     */
    public CategoryDto getCategory(Long categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 종목입니다."));

        BigDecimal openPrice = openPrices.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal currentPrice = currentPrices.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal changeAmount = changeAmounts.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal changeRate = changeRates.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal dailyHigh = dailyHighs.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal dailyLow = dailyLows.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal accVolume = accVolumes.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal accAmount = accAmounts.getOrDefault(categoryId, BigDecimal.ZERO);
        String type = takerType.getOrDefault(categoryId, "");


        return CategoryDto.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .symbol(category.getSymbol())
                .tradePrice(currentPrice)
                .takerType(type)
                .openPrice(openPrice)
                .changeAmount(changeAmount)
                .changeRate(changeRate.setScale(2, RoundingMode.HALF_UP)) // 소수점 2자리 포맷팅
                .dailyHigh(dailyHigh)
                .dailyLow(dailyLow)
                .accVolume(accVolume)
                .accAmount(accAmount)
                .build();
    }

    public List<CategoryDto> getCategories() {
        return categoryRepository.findAll()
                .stream()
                // 삭제되지 않은 종목만 필터링
                .filter(category -> !Boolean.TRUE.equals(category.getCategoryDelete()))
                .map(category -> {
                    Long id = category.getCategoryId();

                    BigDecimal openPrice = openPrices.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal price = currentPrices.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal changeAmount = changeAmounts.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal changeRate = changeRates.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal dailyHigh = dailyHighs.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal dailyLow = dailyLows.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal accVolume = accVolumes.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal accAmount = accAmounts.getOrDefault(id, BigDecimal.ZERO);
                    String type = takerType.getOrDefault(id, "");


                    return CategoryDto.builder()
                            .categoryId(id)
                            .categoryName(category.getCategoryName())
                            .symbol(category.getSymbol())
                            .tradePrice(price)
                            .openPrice(openPrice)
                            .takerType(type)
                            .changeAmount(changeAmount)
                            .changeRate(changeRate.setScale(2, RoundingMode.HALF_UP))
                            .dailyHigh(dailyHigh)
                            .dailyLow(dailyLow)
                            .accVolume(accVolume)
                            .accAmount(accAmount)
                            .build();
                })
                .toList();
    }
}