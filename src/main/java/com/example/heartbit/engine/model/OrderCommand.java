package com.example.heartbit.engine.model;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderType;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OrderCommand {

    private final Long orderId;
    private final BigDecimal orderPrice;
    private BigDecimal remainingCount;
    private final OrderType type;
    private final Long categoryId;

    public OrderCommand(
            Long orderId,
            BigDecimal orderPrice,
            BigDecimal remainingCount,
            OrderType type,
            Long categoryId
    ) {
        this.orderId = orderId;
        this.orderPrice = orderPrice;
        this.remainingCount = remainingCount;
        this.type = type;
        this.categoryId = categoryId;
    }


    public void reduce(BigDecimal remaining) {
        this.remainingCount = this.remainingCount.subtract(remaining);
        if (this.remainingCount.signum() < 0) {
            this.remainingCount = BigDecimal.ZERO;
        }
    }

    public static OrderCommand from(Order order) {
        return new OrderCommand(
                order.getOrderId(),
                order.getOrderPrice(),
                order.getRemainingCount(),
                order.getOrderType(),
                order.getCategory().getCategoryId()
        );
    }
}

