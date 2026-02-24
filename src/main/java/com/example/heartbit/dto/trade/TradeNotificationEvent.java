package com.example.heartbit.dto.trade;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class TradeNotificationEvent {
    private final Long categoryId;
    private final List<TradeResponse> tradeResults;
    private final BigDecimal referencePrice;

    @Getter
    @RequiredArgsConstructor
    public static class NotificationDetail {
        private final Long memberId;
        private final String categoryName;
        private final String type; // "매수" 또는 "매도"
        private final BigDecimal count;
        private final BigDecimal remainingCount;
        private final BigDecimal tradePrice;
    }

    private final List<NotificationDetail> details;
}