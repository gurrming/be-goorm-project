package com.example.heartbit.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class InvestResponse {

    private BigDecimal totalBuyAmount;    // 총 매수
    private BigDecimal totalEvaluation;  // 총 평가
    private BigDecimal totalProfit;      // 총 평가손익
    private BigDecimal totalProfitRate;


    private List<AssetDetailDto> assetList;// 총 평가수익률

    private boolean hasNext;

    private BigDecimal assetCash; //보유 잔액
    private BigDecimal totalAsset; //총 보유 자산

    @Getter
    @Builder
    public static class AssetDetailDto {

        private String categoryName;
        private String symbol;
        private Long categoryId;
        private BigDecimal investCount;  //투자 개수
        private BigDecimal avgPrice;  //매수 평균가
        private BigDecimal buyAmount;  //매수 금액

        private BigDecimal currentPrice;       // 현재가
        private BigDecimal evaluationAmount;     // 평가금액 (현재가 * 수량)
        private BigDecimal evaluationProfit;     // 평가손익 (평가금액 - 매수금액)
        private BigDecimal profitRate;  //수익률


    }
}
