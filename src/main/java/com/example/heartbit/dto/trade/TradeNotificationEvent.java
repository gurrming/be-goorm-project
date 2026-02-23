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
    private final BigDecimal referencePrice; // openPrices에서 가져온 기준가
}