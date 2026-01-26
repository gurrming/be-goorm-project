package com.example.heartbit.dto.order;

import com.example.heartbit.domain.Category;
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

    // 기준값
    private Long categoryId;
    private String symbol;

    // 표시용
    private String categoryName;

    private OrderType orderType;
    private OrderStatus orderStatus;

    private BigDecimal orderPrice;
    private BigDecimal orderCount;
    private BigDecimal remainingCount;
    private BigDecimal executedCount;

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

        Category category = order.getCategory();

        return OrderResponse.builder()
                .orderId(order.getOrderId())

                // 기준값
                .categoryId(category != null ? category.getCategoryId() : null)
                .symbol(category != null ? category.getSymbol() : null)

                // 표시용
                .categoryName(category != null ? category.getCategoryName() : "알 수 없음")
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
