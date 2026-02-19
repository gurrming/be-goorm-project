package com.example.heartbit.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class AnalysisDTO {
    private String symbol;
    private Double totalResult;
    private String totalLabel;     // BUY, SELL, HOLD
    private String summary;        // [추가] 한 줄 요약
    private Double rsi;            // [추가] RSI
    private Double newsResult;
    private Double communityResult;
    private String fullReport;  // 상세 리포트
}