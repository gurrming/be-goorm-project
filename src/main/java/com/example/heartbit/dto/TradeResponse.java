package com.example.heartbit.dto;

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
    private LocalDateTime tradeTime;
    private BigDecimal tradePrice;
    private BigDecimal tradeCount;
    private BigDecimal tradeClosePrice;

    private Long buyOrderId;
    private Long sellOrderId;
    // 추가
    private String takerType;

    @Builder
    private TradeResponse(
            Long tradeId,
            LocalDateTime tradeTime,
            BigDecimal tradePrice,
            BigDecimal tradeCount,
            BigDecimal tradeClosePrice,
            Long buyOrderId,
            Long sellOrderId,
            String takerType
    ) {
        this.tradeId = tradeId;
        this.tradeTime = tradeTime;
        this.tradePrice = tradePrice;
        this.tradeCount = tradeCount;
        this.tradeClosePrice = tradeClosePrice;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.takerType = takerType;
    }

    public static TradeResponse fromEntity(Trade trade) {
        return TradeResponse.builder()
                .tradeId(trade.getTradeId())
                .tradeTime(trade.getTradeTime())
                .tradePrice(trade.getTradePrice())
                .tradeCount(trade.getTradeCount())
                .tradeClosePrice(trade.getTradeClosePrice())
                .buyOrderId(trade.getBuyOrder().getOrderId())
                .sellOrderId(trade.getSellOrder().getOrderId())
                .build();
    }
}
