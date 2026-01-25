package com.example.heartbit.dto.order;

import com.example.heartbit.domain.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EngineMatchResult {
    private final Order taker;  // 새로 들어온 주문 객체
    private final Order maker;  // 기존에 대기 중이던 주문 객체
    private final BigDecimal tradePrice;
    private final BigDecimal tradeCount;
    private final LocalDateTime tradeTime;

}
