package com.example.heartbit.dto;

import com.example.heartbit.domain.Order;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class TradeRequest {

    @NotNull
    private BigDecimal tradePrice;
    @NotNull
    private BigDecimal tradeCount;
    @NotNull
    private Long categoryId;
    @NotNull
    private Long buyOrderId;
    @NotNull
    private Long sellOrderId;
    @NotNull
    private LocalDateTime tradeTime;


}
