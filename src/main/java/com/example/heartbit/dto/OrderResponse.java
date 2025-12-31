package com.example.heartbit.dto;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderStatus;
import com.example.heartbit.domain.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String categoryName;
    private OrderType orderType;
    private OrderStatus orderStatus;

    private BigDecimal orderPrice;
    private BigDecimal orderCount; // 전체 주문 수량
    private BigDecimal remainingCount; // 남은 수량
    private BigDecimal executedCount;  // 체결된 수량

    private BigDecimal totalAmount;
    private String orderTime;

    public static OrderResponse from(Order order) {

        BigDecimal total = order.getOrderCount();
        BigDecimal remaining = order.getRemainingCount();
        BigDecimal executed = total.subtract(remaining);

        BigDecimal totalAmount = order.getOrderPrice().multiply(total);

        String formattedTime = order.getOrderTime() != null
                ? order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "";

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .categoryName(order.getCategory() != null ? order.getCategory().getCategoryName() : "알 수 없음")
                .orderType(order.getOrderType())
                .orderStatus(order.getOrderStatus())
                .orderPrice(order.getOrderPrice())
                .orderCount(total)
                .remainingCount(remaining)
                .executedCount(executed)
                .totalAmount(totalAmount)
                .orderTime(formattedTime)
                .build();
    }
}
