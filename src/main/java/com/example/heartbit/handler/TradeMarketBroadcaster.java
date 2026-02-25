package com.example.heartbit.handler;

import com.example.heartbit.dto.trade.TradeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeMarketBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

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

    private final Map<Long, BigDecimal> candleOpens = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> candleHighs = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> candleLows = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> currentMinutes = new ConcurrentHashMap<>();

    public void updateMarketAndBroadcast(Long categoryId, TradeResponse response) {
        BigDecimal price = response.getTradePrice();
        BigDecimal count = response.getTradeCount();

        // 시가 결정(단순화)
        BigDecimal openPrice = openPrices.computeIfAbsent(categoryId, k -> price);
        if (openPrice.compareTo(BigDecimal.ZERO) == 0) {
            openPrice = price;
            openPrices.put(categoryId, price);
        }

        BigDecimal changeAmount = price.subtract(openPrice);
        BigDecimal changeRate = (openPrice.compareTo(BigDecimal.ZERO) == 0)
                ? BigDecimal.ZERO
                : changeAmount.divide(openPrice, 10, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

        takerType.put(categoryId, response.getTakerType());
        currentPrices.put(categoryId, price);
        changeAmounts.put(categoryId, changeAmount);
        changeRates.put(categoryId, changeRate);

        dailyHighs.merge(categoryId, price, (old, val) -> val.compareTo(old) > 0 ? val : old);
        dailyLows.merge(categoryId, price, (old, val) -> val.compareTo(old) < 0 ? val : old);

        accVolumes.merge(categoryId, count, BigDecimal::add);
        accAmounts.merge(categoryId, price.multiply(count), BigDecimal::add);

        if ("BUY".equals(response.getTakerType())) totalBuyQtys.merge(categoryId, count, BigDecimal::add);
        else totalSellQtys.merge(categoryId, count, BigDecimal::add);

        updateCandle(categoryId, price, response.getTradeTime());
        sendWebSocketData(categoryId, response);
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

    private void sendWebSocketData(Long categoryId, TradeResponse response) {
        String suffix = "/" + categoryId;
        BigDecimal price = response.getTradePrice();

        BigDecimal buyQty = totalBuyQtys.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal sellQty = totalSellQtys.getOrDefault(categoryId, BigDecimal.ONE);
        if (sellQty.compareTo(BigDecimal.ZERO) == 0) sellQty = BigDecimal.ONE;

        BigDecimal intensity = buyQty.divide(sellQty, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        BigDecimal openPrice = openPrices.getOrDefault(categoryId, price);

        BigDecimal dailyHigh = dailyHighs.getOrDefault(categoryId, price);
        BigDecimal dailyLow = dailyLows.getOrDefault(categoryId, price);

        //
        Map<String, Object> ticker = new HashMap<>();
        ticker.put("price", price.toPlainString());
        ticker.put("changeAmount", price.subtract(openPrice).toPlainString());
        ticker.put("changeRate", changeRates.getOrDefault(categoryId, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString());
        ticker.put("high", dailyHigh.toPlainString());
        ticker.put("low", dailyLow.toPlainString());
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

        Map<String, Object> lastPrice = new HashMap<>();
        lastPrice.put("price", price.toPlainString());
        messagingTemplate.convertAndSend("/topic/orderbook/lastPrice/" + categoryId, (Object)lastPrice);
    }
}
