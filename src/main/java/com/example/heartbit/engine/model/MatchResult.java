package com.example.heartbit.engine.model;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MatchResult {
    private Long buyOrderId;
    private Long sellOrderId;
    private BigDecimal price;
    private BigDecimal orderCount;
}


