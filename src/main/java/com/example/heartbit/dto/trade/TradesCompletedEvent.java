package com.example.heartbit.dto.trade;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class TradesCompletedEvent {
    private Long categoryId;
    private List<TradeDetail> tradeDetails;

    @Getter
    @AllArgsConstructor
    public static class TradeDetail {
        private Long buyerId;
        private Long sellerId;
        private BigDecimal tradePrice;
        private BigDecimal tradeCount;
        private Long tradeId;
    }
}
