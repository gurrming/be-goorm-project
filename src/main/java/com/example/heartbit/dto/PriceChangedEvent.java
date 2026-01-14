package com.example.heartbit.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class PriceChangedEvent {
    // 어떤 종목인지
    private final Long categoryId;
    // 얼마로 변했는지
    private final BigDecimal newPrice;
}