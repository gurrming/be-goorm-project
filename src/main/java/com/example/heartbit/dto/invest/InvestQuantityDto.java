package com.example.heartbit.dto.invest;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@AllArgsConstructor
public class InvestQuantityDto {

    @Schema(description = "종목 아이디")
    private Long categoryId;

    @Schema(description = "종목 이름")
    private String categoryName;

    @Schema(description = "심볼")
    private String symbol;

    @Schema(description = "보유 수량")
    private BigDecimal quantity;
}

