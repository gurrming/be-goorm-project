package com.example.heartbit.dto;

import com.example.heartbit.domain.Asset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class InvestResponse {


    private BigDecimal totalBuyingAmount;
    private BigDecimal totalEvaluation;
    private BigDecimal orderableAmount;
    private BigDecimal totalProfitLoss;
    private Double totalProfitRate;

    private List<HoldingStockResponse> holdingStocks;


}