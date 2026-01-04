package com.example.heartbit.service;

import com.example.heartbit.domain.Trade;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.repository.TradeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private BigDecimal openPrice = BigDecimal.ZERO;       // 기준가 (오전 9시 종가)
    private BigDecimal currentPrice = BigDecimal.ZERO;    // 현재가
    private BigDecimal dailyHigh = BigDecimal.ZERO;      // 당일 고가
    private BigDecimal dailyLow = new BigDecimal("999999999999999999"); // 당일 저가 초기값
    private BigDecimal accVolume = BigDecimal.ZERO;      // 당일 누적 거래량
    private BigDecimal accAmount = BigDecimal.ZERO;      // 당일 누적 거래대금
    private BigDecimal totalBuyQty = BigDecimal.ZERO;    // 체결강도용 매수량
    private BigDecimal totalSellQty = BigDecimal.ZERO;    //체결강도용 매도량


    private BigDecimal candleOpen = BigDecimal.ZERO;
    private BigDecimal candleHigh = BigDecimal.ZERO;
    private BigDecimal candleLow = new BigDecimal("999999999999999999");
    private LocalDateTime currentMinute = LocalDateTime.now().withSecond(0).withNano(0);


    //오전 9시마다 장 갱신
    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = now.withHour(9).withMinute(0).withSecond(0).withNano(0);

        if (now.isBefore(targetTime)) {
            targetTime = targetTime.minusDays(1);
        }

        //오전 9시 정각 이전에 발생한 체결중 9시에 가장 가까운 마지막 체결가 가져오기
        this.openPrice = tradeRepository.findTop1ByTradeTimeBeforeOrderByTradeTimeDesc(targetTime)
                .map(Trade::getTradePrice)
                .orElse(new BigDecimal("131000000.00000000"));

        this.currentPrice = this.openPrice;
        log.info("오전 9시 기준가 로드 완료: {} (기준시점: {})", openPrice.toPlainString(), targetTime);
    }

    @Transactional
    public void processTrade(Trade tradeResult) {
        BigDecimal price = tradeResult.getTradePrice(); //체결 엔진으로부터 나온 체결가
        BigDecimal count = tradeResult.getTradeCount(); //체결 엔진으로부터 나온 체결개수

        // Taker(주문을 던진 쪽) 판별:
        // 체결 엔진 로직상 나중에 들어온 주문(Taker)의 시간을 기준으로 Trade가 생성되므로
        // buyOrder와 sellOrder 중 tradeTime과 더 가까운 쪽을 찾거나
        // 엔진에서 taker 정보를 넘겨주는 것이 정확하나, 여기서는 수량 기반/시간 기반 로직을 활용
        // 일반적인 클론 코딩에서는 buyOrder가 나중에 생성되었으면 매수세로 봅니다.
        boolean isBuyTaker = tradeResult.getBuyOrder().getOrderTime().isAfter(tradeResult.getSellOrder().getOrderTime());

        //현재가 및 당일 고가/저가 업데이트
        this.currentPrice = price;
        if (price.compareTo(dailyHigh) > 0) dailyHigh = price;
        if (price.compareTo(dailyLow) < 0) dailyLow = price;

        // 24시간 누적 거래량 및 거래대금
        accVolume = accVolume.add(count);
        accAmount = accAmount.add(price.multiply(count));

        // 변동금/변동률 계산
        BigDecimal changeAmount = price.subtract(openPrice);
        BigDecimal changeRate = BigDecimal.ZERO;
        if (openPrice.compareTo(BigDecimal.ZERO) != 0) {
            changeRate = changeAmount.divide(openPrice, 10, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        // 체결강도 계산
        if (isBuyTaker) totalBuyQty = totalBuyQty.add(count);
        else totalSellQty = totalSellQty.add(count);

        BigDecimal intensity = new BigDecimal("100.00");
        if (totalSellQty.compareTo(BigDecimal.ZERO) != 0) {
            intensity = totalBuyQty.divide(totalSellQty, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        // 1분봉 데이터 업데이트 및 브로드캐스트
        updateCandle(price);
        broadcastData(tradeResult, changeAmount, changeRate, intensity, isBuyTaker);
    }

    private void broadcastData(Trade trade, BigDecimal changeAmount, BigDecimal changeRate, BigDecimal intensity, boolean isBuyTaker) {
        // Ticker (상단 시세 + 우측 배너)
        Map<String, Object> ticker = new HashMap<>();
        ticker.put("price", trade.getTradePrice().toPlainString());
        ticker.put("changeAmount", changeAmount.toPlainString());
        ticker.put("changeRate", changeRate.setScale(2, RoundingMode.HALF_UP).toPlainString());
        ticker.put("high", dailyHigh.toPlainString());
        ticker.put("low", dailyLow.toPlainString());
        ticker.put("volume", accVolume.setScale(8, RoundingMode.HALF_UP).toPlainString());
        ticker.put("amount", accAmount.setScale(0, RoundingMode.HALF_UP).toPlainString());
        ticker.put("intensity", intensity.setScale(2, RoundingMode.HALF_UP).toPlainString());
        messagingTemplate.convertAndSend("/topic/ticker", (Object)ticker);

        // 실시간 체결 내역 (왼쪽 하단)
        Map<String, Object> tradeList = new HashMap<>();
        tradeList.put("price", trade.getTradePrice().toPlainString());
        tradeList.put("count", trade.getTradeCount().toPlainString());
        tradeList.put("type", isBuyTaker ? "BUY" : "SELL");
        tradeList.put("time", trade.getTradeTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        messagingTemplate.convertAndSend("/topic/trades", (Object)tradeList);

        // 검정 테두리용 현재가
        messagingTemplate.convertAndSend("/topic/orderbook/lastPrice", trade.getTradePrice().toPlainString());

        // 차트용 OHLC
        Map<String, Object> candle = new HashMap<>();
        candle.put("t", currentMinute.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        candle.put("o", candleOpen.toPlainString());
        candle.put("h", candleHigh.toPlainString());
        candle.put("l", candleLow.toPlainString());
        candle.put("c", trade.getTradePrice().toPlainString());
        messagingTemplate.convertAndSend("/topic/charts", (Object)candle);
    }

    public List<TradeResponse> getTradesForChart(Long categoryId) {
        // 현재 시간 기준 15분 전 시점 계산
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);

        // Repository에서 15분 전부터 지금까지의 모든 체결 내역 조회
        // findByTradeTimeAfterOrderByTradeTimeAsc 메서드 필요
        return tradeRepository.findByTradeTimeAfterOrderByTradeTimeAsc(fifteenMinutesAgo).stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }



    public List<TradeResponse> getTradeList(Long categoryId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return tradeRepository.findTopTradesByCategory(categoryId, pageable).stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 특정 종목의 현재가 1건 조회
    public TradeResponse getRecentTrade(Long categoryId) {
        return tradeRepository.findTop1ByCategoryOrderByTradeTimeDesc(categoryId)
                .map(TradeResponse::fromEntity)
                .orElse(null);
    }

    public List<TradeResponse> getTradeByOrder(Long orderId) {
        // 1. 해당 주문 ID가 매수 주문이었든 매도 주문이었든 연관된 모든 체결을 가져옴
        List<Trade> trades = tradeRepository.findByBuyOrder_OrderIdOrSellOrder_OrderId(orderId, orderId);

        // 2. DTO 리스트로 변환
        return trades.stream()
                .map(trade -> {
                    // 이 주문(orderId)이 Taker인지 Maker인지 판별하여 타입을 결정할 수 있습니다.
                    // 여기서는 엔티티의 생성 시간 등을 비교하여 Taker 여부를 판단하거나 단순 변환합니다.
                    return TradeResponse.fromEntity(trade);
                })
                .collect(Collectors.toList());
    }

    public List<TradeResponse> getMyTrade(Long memberId, int page, int size) {
        // 1. 최신순으로 정렬된 페이징 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // 2. Repository에서 Page 객체로 결과 수신
        Page<Trade> tradePage = tradeRepository.findTradeByMemberId(memberId, pageable);

        // 3. DTO 리스트로 변환하여 반환
        return tradePage.getContent().stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void updateCandle(BigDecimal price) {
        LocalDateTime nowMinute = LocalDateTime.now().withSecond(0).withNano(0);
        if (nowMinute.isAfter(currentMinute)) {
            candleOpen = price;
            candleHigh = price;
            candleLow = price;
            currentMinute = nowMinute;
        } else {
            if (price.compareTo(candleHigh) > 0) candleHigh = price;
            if (price.compareTo(candleLow) < 0) candleLow = price;
        }
    }

    //오전 9시되면 모든 값 초기화
    @Scheduled(cron = "0 0 9 * * *")
    public void refreshMarket() {
        this.openPrice = this.currentPrice;
        this.dailyHigh = currentPrice;
        this.dailyLow = currentPrice;
        this.accVolume = BigDecimal.ZERO;
        this.accAmount = BigDecimal.ZERO;
        this.totalBuyQty = BigDecimal.ZERO;
        this.totalSellQty = BigDecimal.ZERO;
    }

}
