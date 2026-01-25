package com.example.heartbit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
@AllArgsConstructor
public class CategoryDto {

    private Long categoryId;
    private String categoryName;
    private String symbol;
    private BigDecimal openPrice;
    private String takerType;
    private BigDecimal tradePrice;
    private BigDecimal changeRate;
    private BigDecimal changeAmount;
    private BigDecimal dailyHigh;
    private BigDecimal dailyLow;
    private BigDecimal changeRateHigh;
    private BigDecimal changeRateLow;
    private BigDecimal accVolume;
    private BigDecimal accAmount;

}
