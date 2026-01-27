package com.example.heartbit.dto;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.domain.Trade;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@NoArgsConstructor
public class TradeResponse {

    private Long tradeId;
    private Long categoryId;
    private String symbol;
    private LocalDateTime tradeTime;
    private BigDecimal tradePrice;
    private BigDecimal tradeCount;
    private BigDecimal tradeClosePrice;

    private Long buyOrderId;
    private Long sellOrderId;

    private Order buyOrderEntity;
    private Order sellOrderEntity;
    // 추가
    private String takerType;

    private OrderType myOrderType; // 내 체결이 BUY/SELL


    @Builder
    private TradeResponse(
            Long tradeId,
            LocalDateTime tradeTime,
            BigDecimal tradePrice,
            BigDecimal tradeCount,
            BigDecimal tradeClosePrice,
            String symbol,
            Long buyOrderId,
            Long sellOrderId,
            String takerType,
            OrderType myOrderType,
            Order buyOrderEntity,
            Order sellOrderEntity
    ) {
        this.tradeId = tradeId;
        this.tradeTime = tradeTime;
        this.tradePrice = tradePrice;
        this.tradeCount = tradeCount;
        this.tradeClosePrice = tradeClosePrice;
        this.symbol = symbol;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.takerType = takerType;
        this.myOrderType = myOrderType;
        this.buyOrderEntity = buyOrderEntity;
        this.sellOrderEntity = sellOrderEntity;
    }


    public static TradeResponse fromEntity(Trade trade) {

        return TradeResponse.builder()
                .tradeId(trade.getTradeId())
                .tradeTime(trade.getTradeTime())
                .tradePrice(trade.getTradePrice())
                .tradeCount(trade.getTradeCount())
                .tradeClosePrice(trade.getTradeClosePrice())
                .symbol(trade.getSymbol())
                .takerType(trade.getTakerType())
                .buyOrderId(trade.getBuyOrder().getOrderId())
                .sellOrderId(trade.getSellOrder().getOrderId())
                .build();
    }

    public static TradeResponse fromEntityWithOrderType(Trade trade, Long memberId) {

        OrderType myOrderType = null;


        // 1. 매수 주문 확인 (봇일 수 있으므로 getMember() null 체크 필수)
        if (trade.getBuyOrder().getMember() != null &&
                trade.getBuyOrder().getMember().getMemberId().equals(memberId)) {
            myOrderType = OrderType.BUY;
        }
        // 2. 매도 주문 확인 (매도 주문의 memberId 비교)
        else if (trade.getSellOrder().getMember() != null &&
                trade.getSellOrder().getMember().getMemberId().equals(memberId)) {
            myOrderType = OrderType.SELL;
        }

        String takerType = trade.getBuyOrder().getOrderTime().isAfter(trade.getSellOrder().getOrderTime())
                ? "BUY" : "SELL";

        return TradeResponse.builder()
                .tradeId(trade.getTradeId())
                .tradeTime(trade.getTradeTime())
                .tradePrice(trade.getTradePrice())
                .tradeCount(trade.getTradeCount())
                .tradeClosePrice(trade.getTradeClosePrice())
                .symbol(trade.getSymbol())
                .buyOrderId(trade.getBuyOrder().getOrderId())
                .sellOrderId(trade.getSellOrder().getOrderId())
                .takerType(takerType)
                .myOrderType(myOrderType)
                .build();
    }

}
