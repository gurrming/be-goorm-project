package com.example.heartbit.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class HoldingStockResponse {
    private String categoryName;
    private BigDecimal quantity;
    private BigDecimal avgBuyPrice;
    private BigDecimal buyAmount;
    private BigDecimal evalAmount;
    private BigDecimal profitLoss;
}
