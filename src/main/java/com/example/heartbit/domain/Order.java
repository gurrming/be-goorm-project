package com.example.heartbit.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_price", precision = 18, scale = 8, nullable = false)
    private BigDecimal orderPrice;

    @Column(name = "order_count", precision = 18, scale = 8, nullable = false)
    private BigDecimal orderCount;

    @Column(name = "remaining_count", precision = 18, scale = 8, nullable = false)
    private BigDecimal remainingCount;

    @CreationTimestamp
    @Column(name = "order_time", updatable = false)
    private LocalDateTime orderTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 10)
    private OrderStatus orderStatus;

    @Column(name = "is_bot", nullable = false)
    private Boolean isBot = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Builder
    public Order(BigDecimal orderPrice,
                 BigDecimal orderCount,
                 BigDecimal remainingCount,
                 OrderType orderType,
                 OrderStatus orderStatus,
                 Boolean isBot,
                 Member member,
                 Category category) {

        this.orderPrice = orderPrice;
        this.orderCount = orderCount;
        this.remainingCount = (remainingCount != null) ? remainingCount : orderCount;
        this.orderType = orderType;
        this.orderStatus = (orderStatus != null) ? orderStatus : OrderStatus.OPEN;
        this.isBot = (isBot != null) ? isBot : false;
        this.member = member;
        this.category = category;
    }

    public void updateRemainingCount(BigDecimal executedCount) {
        this.remainingCount = this.remainingCount.subtract(executedCount);
        if (this.remainingCount.compareTo(BigDecimal.ZERO) == 0) {
            this.orderStatus = OrderStatus.FILLED;
        } else {
            this.orderStatus = OrderStatus.PARTIAL;
        }
    }

    public void cancel() {
        if (this.orderStatus == OrderStatus.FILLED) {
            throw new IllegalStateException("이미 체결된 주문은 취소할 수 없습니다.");
        }
        this.orderStatus = OrderStatus.CANCELLED;
    }
}
