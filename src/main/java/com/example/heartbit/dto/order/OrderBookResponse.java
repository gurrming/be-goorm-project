package com.example.heartbit.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class OrderBookResponse {
    // 호가
    private BigDecimal orderPrice;
    // 해당 가격의 총 잔량 합계
    private BigDecimal totalRemainingCount;

}
