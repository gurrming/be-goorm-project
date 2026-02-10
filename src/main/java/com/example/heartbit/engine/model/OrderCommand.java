package com.example.heartbit.engine.model;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderType;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OrderCommand {

    private final Long orderId;
    private final BigDecimal orderPrice;
    private final OrderType type;
    private final Long categoryId;
    private BigDecimal remainingCount;

    public OrderCommand(
            Long orderId,
            BigDecimal orderPrice,
            OrderType type,
            Long categoryId,
            BigDecimal remainingCount
            ) {
        this.orderId = orderId;
        this.orderPrice = orderPrice;
        this.type = type;
        this.categoryId = categoryId;
        this.remainingCount = remainingCount;
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
                order.getOrderType(),
                order.getCategory().getCategoryId(),
                order.getRemainingCount()
        );
    }
}

