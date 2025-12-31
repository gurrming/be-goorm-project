package com.example.heartbit.dto.invest;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class InvestSummaryDto {

    private BigDecimal totalBuyAmount; // 총 매수 금액
    private BigDecimal totalEvaluateAmount; // 총 평가 금액
    private BigDecimal totalProfit; // 총 평가 손익
    private BigDecimal totalProfitRate; // 총 수익률
}
