package com.example.heartbit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class TradeRequest {

    @NotNull
    private BigDecimal tradePrice;
    @NotNull
    private BigDecimal tradeCount;
    @NotNull
    private Long buyOrderId;
    @NotNull
    private Long sellOrderId;

    @NotNull
    private LocalDateTime tradeTime;

    @NotNull
    private String takerType;
}
