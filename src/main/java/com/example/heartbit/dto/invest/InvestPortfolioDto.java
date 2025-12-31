package com.example.heartbit.dto.invest;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InvestPortfolioDto {

    private InvestSummaryDto summary;
    private List<InvestAssetDto> assets;
}

