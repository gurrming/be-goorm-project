package com.example.heartbit.dto.invest;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class InvestQuantityDto {
    private Long categoryId;       // 종목 아이디
    private String categoryName;   // 종목 이름
    private String symbol;         // 심볼
    private BigDecimal quantity;   // 수량
}
