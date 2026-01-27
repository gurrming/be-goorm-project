package com.example.heartbit.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class AnalysisDTO {
    private String symbol;          // Category 테이블에서 가져올 심볼 (예: BTC)
    private Double totalResult;
    private String totalLabel;
    private Double newsResult;
    private Double communityResult;
}