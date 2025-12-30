package com.example.heartbit.dto;

import com.example.heartbit.domain.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class OrderRequest {

    private Long memberId;
    private Long categoryId;
    private BigDecimal price;
    private BigDecimal quantity;
    private OrderType type;



    public Order toEntity(Member member, Category category) {
        return Order.builder()
                .orderPrice(this.price)
                .orderCount(this.quantity)
                .orderType(this.type)
                .orderStatus(OrderStatus.OPEN)
                .member(member)
                .category(category)
                .build();
    }
}
