package com.example.heartbit.dto;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Member;
import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor // JSON 데이터를 객체로 변환할 때 필요할 수 있음
public class OrderRequest {

    @NotNull(message = "회원 ID는 필수입니다.")
    private Long memberId;

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    @NotNull(message = "가격은 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = false, message = "가격은 0보다 커야 합니다.")
    private BigDecimal orderPrice;

    @NotNull(message = "수량은 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = false, message = "수량은 0보다 커야 합니다.")
    private BigDecimal orderCount;

    @NotNull(message = "주문 타입(BUY/SELL)은 필수입니다.")
    private OrderType type;

    public Order toEntity(Member member, Category category) {
        return Order.builder()
                .orderPrice(this.orderPrice)
                .orderCount(this.orderCount)
                .orderType(this.type)
                .member(member)
                .category(category)
                .build();
    }
}