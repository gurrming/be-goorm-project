package com.example.heartbit.service;

import com.example.heartbit.domain.Trade;
import com.example.heartbit.dto.TradeRequest;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.repository.TradeRepository;
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

    // 실시간 시세 상태 관리 변수들
    private BigDecimal openPrice = BigDecimal.ZERO;  //장시작가
    private BigDecimal currentPrice = BigDecimal.ZERO;  //현재가
    private BigDecimal dailyHigh = BigDecimal.ZERO;  //당일고가
    private BigDecimal dailyLow = new BigDecimal("999999999999999999");  //당일저가
    private BigDecimal accVolume = BigDecimal.ZERO;  //누적거래량
    private BigDecimal accAmount = BigDecimal.ZERO;  //누적거래금
    private BigDecimal totalBuyQty = BigDecimal.ZERO;  //매수체결량
    private BigDecimal totalSellQty = BigDecimal.ZERO;  //매도체결량

    // 차트용 변수
    private BigDecimal candleOpen = BigDecimal.ZERO;  //시가
    private BigDecimal candleHigh = BigDecimal.ZERO;  //고가
    private BigDecimal candleLow = new BigDecimal("999999999999999999");  //저가
    private LocalDateTime currentMinute = LocalDateTime.now().withSecond(0).withNano(0);



    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = now.withHour(9).withMinute(0).withSecond(0).withNano(0);
        if (now.isBefore(targetTime)) targetTime = targetTime.minusDays(1);


        this.openPrice = tradeRepository.findTop1ByTradeTimeBeforeOrderByTradeTimeDesc(targetTime)
                .map(Trade::getTradePrice)
                .orElse(new BigDecimal("131000000"));

        this.currentPrice = this.openPrice;
        log.info("기준가 로드 완료: {} (기준시점: {})", openPrice.toPlainString(), targetTime);
    }

    /**
     * 체결 처리 핵심 로직
     * @param request 엔진으로부터 넘어온 DTO
     */
    @Transactional
    public void processTrade(TradeRequest request) {
        BigDecimal price = request.getTradePrice();
        BigDecimal count = request.getTradeCount();

        // 1. Taker 판별 (엔티티 조회 없이 DTO에서 직접 확인)
        boolean isBuyTaker = "BUY".equals(request.getTakerType());

        // 2. 현재가 및 당일 고가/저가 업데이트
        this.currentPrice = price;
        if (price.compareTo(dailyHigh) > 0) dailyHigh = price;
        if (price.compareTo(dailyLow) < 0) dailyLow = price;

        // 3. 거래량 및 거래대금 누적
        accVolume = accVolume.add(count);
        accAmount = accAmount.add(price.multiply(count));

        // 4. 변동 금액/변동률 계산
        BigDecimal changeAmount = price.subtract(openPrice);
        BigDecimal changeRate = BigDecimal.ZERO;
        if (openPrice.compareTo(BigDecimal.ZERO) != 0) {
            changeRate = changeAmount.divide(openPrice, 10, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        // 5. 체결강도 계산
        if (isBuyTaker) totalBuyQty = totalBuyQty.add(count);
        else totalSellQty = totalSellQty.add(count);

        BigDecimal intensity = new BigDecimal("100.00");
        if (totalSellQty.compareTo(BigDecimal.ZERO) != 0) {
            intensity = totalBuyQty.divide(totalSellQty, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        // 6. 1분봉 데이터 업데이트 (엔진이 준 시간 기준)
        updateCandle(price, request.getTradeTime());

        // 7. 실시간 데이터 전송 (DTO 기반으로 수정)
        broadcastData(request, changeAmount, changeRate, intensity, isBuyTaker);
    }

    private void broadcastData(TradeRequest request, BigDecimal changeAmount, BigDecimal changeRate, BigDecimal intensity, boolean isBuyTaker) {
        // Ticker 정보 (상단바)
        Map<String, Object> ticker = new HashMap<>();
        ticker.put("price", request.getTradePrice().toPlainString());
        ticker.put("changeAmount", changeAmount.toPlainString());
        ticker.put("changeRate", changeRate.setScale(2, RoundingMode.HALF_UP).toPlainString());
        ticker.put("high", dailyHigh.toPlainString());
        ticker.put("low", dailyLow.toPlainString());
        ticker.put("volume", accVolume.setScale(8, RoundingMode.HALF_UP).toPlainString());
        ticker.put("amount", accAmount.setScale(0, RoundingMode.HALF_UP).toPlainString());
        ticker.put("intensity", intensity.setScale(2, RoundingMode.HALF_UP).toPlainString());
        messagingTemplate.convertAndSend("/topic/ticker", (Object)ticker);

        // 실시간 체결 내역 (왼쪽 하단 리스트)
        Map<String, Object> tradeList = new HashMap<>();
        tradeList.put("price", request.getTradePrice().toPlainString());
        tradeList.put("count", request.getTradeCount().toPlainString());
        tradeList.put("type", isBuyTaker ? "BUY" : "SELL");
        tradeList.put("time", request.getTradeTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        messagingTemplate.convertAndSend("/topic/trades", (Object)tradeList);

        // 오더북 현재가 테두리용
        messagingTemplate.convertAndSend("/topic/orderbook/lastPrice", request.getTradePrice().toPlainString());

        // 차트용 OHLC 데이터
        Map<String, Object> candle = new HashMap<>();
        candle.put("t", currentMinute.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        candle.put("o", candleOpen.toPlainString());
        candle.put("h", candleHigh.toPlainString());
        candle.put("l", candleLow.toPlainString());
        candle.put("c", request.getTradePrice().toPlainString());
        messagingTemplate.convertAndSend("/topic/charts", (Object)candle);
    }

    // 차트용 15분 데이터
    public List<TradeResponse> getTradesForChart(Long categoryId) {
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
        return tradeRepository.findByTradeTimeAfterOrderByTradeTimeAsc(fifteenMinutesAgo).stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 종목별 최신 리스트 (limit개)
    public List<TradeResponse> getTradeList(Long categoryId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return tradeRepository.findTopTradesByCategory(categoryId, pageable).stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 현재가 (최신 1건) 조회
    public TradeResponse getRecentTrade(Long categoryId) {
        return tradeRepository.findTop1ByCategoryOrderByTradeTimeDesc(categoryId)
                .map(TradeResponse::fromEntity)
                .orElse(null);
    }

    // 특정 주문의 체결 내역
    public List<TradeResponse> getTradeByOrder(Long orderId) {
        return tradeRepository.findByBuyOrder_OrderIdOrSellOrder_OrderId(orderId, orderId).stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 내 체결 내역 (페이징)
    public List<TradeResponse> getMyTrade(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return tradeRepository.findTradeByMemberId(memberId, pageable).getContent().stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    //캔들 업데이트
    private void updateCandle(BigDecimal price, LocalDateTime tradeTime) {
        // 엔진이 준 시간을 기준으로 분 단위 절삭
        LocalDateTime nowMinute = tradeTime.withSecond(0).withNano(0);

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

    // 9시가 되면 모든 값 초기화
    @Scheduled(cron = "0 0 9 * * *")
    public void refreshMarket() {
        this.openPrice = this.currentPrice;
        this.dailyHigh = currentPrice;
        this.dailyLow = currentPrice;
        this.accVolume = BigDecimal.ZERO;
        this.accAmount = BigDecimal.ZERO;
        this.totalBuyQty = BigDecimal.ZERO;
        this.totalSellQty = BigDecimal.ZERO;
        log.info("장 시작 - 기준가 갱신: {}", openPrice);
    }

    // TradeResponse.fromEntity를 사용하여 반환하도록 처리
}