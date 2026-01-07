package com.example.heartbit.dto.invest;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class InvestAssetDto {

    private Long categoryId;
    private String categoryName;
    private String symbol;

    private BigDecimal quantity;          // 보유수량
    private BigDecimal avgBuyPrice;   // 매수평균가
    private BigDecimal buyAmount;          // 매수금액
    private BigDecimal evaluateAmount;     // 평가금액
    private BigDecimal profit;             // 평가손익
}
