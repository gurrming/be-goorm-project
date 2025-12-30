package com.example.heartbit.dto;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String categoryName; // "ETH", "BTC" 등 종목명
    private OrderType orderType;  // BUY, SELL
    private OrderStatus orderStatus; // OPEN, PARTIAL, FILLED, CANCELLED

    private BigDecimal price;      // 주문 가격
    private BigDecimal totalCount; // 전체 주문 수량
    private BigDecimal remainingCount; // 남은 수량 (미체결 수량)
    private BigDecimal executedCount;  // 체결된 수량 (total - remaining)

    private BigDecimal totalAmount;    // 총 주문 금액 (price * totalCount)
    private String orderTime;          // 주문 시간 (포맷팅된 문자열)

    // Entity -> DTO 변환 정적 메서드
    public static OrderResponse from(Order order) {
        // 체결된 수량 계산
        BigDecimal executed = order.getOrdersCount().subtract(order.getRemainingCount());

        return OrderResponse.builder()
                .orderId(order.getOrdersId())
                .categoryName(order.getCategory().getName()) // Category 엔티티에 name 필드가 있다고 가정
                .orderType(order.getOrderType())
                .orderStatus(order.getOrderStatus())
                .price(order.getOrdersPrice())
                .totalCount(order.getOrdersCount())
                .remainingCount(order.getRemainingCount())
                .executedCount(executed)
                .totalAmount(order.getOrdersPrice().multiply(order.getOrdersCount()))
                .orderTime(order.getOrdersTime().toString())
                .build();
    }
}
