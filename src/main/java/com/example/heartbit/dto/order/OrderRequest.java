package com.example.heartbit.dto.order;

import com.example.heartbit.domain.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {


    private Long memberId;
    private Long botId;

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    @NotNull(message = "가격은 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = false, message = "가격은 0보다 커야 합니다.")
    private BigDecimal orderPrice;

    @NotNull(message = "수량은 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = false, message = "수량은 0보다 커야 합니다.")
    private BigDecimal orderCount;

    @NotNull(message = "주문 타입(BUY/SELL)은 필수입니다.")
    private OrderType orderType;



    // DTO → Entity 변환
    public Order toEntity(Member member, Category category, Bots bots) {
        return Order.builder()
                .orderPrice(this.orderPrice)
                .orderCount(this.orderCount)
                .remainingCount(this.orderCount)
                .orderType(this.orderType)
                .orderStatus(OrderStatus.OPEN)
                .member(member)
                .category(category)
                .bots(bots)
                .build();
    }
}
