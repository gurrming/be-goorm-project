package com.example.heartbit.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.http.converter.json.GsonBuilderUtils;

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


    @Column(name = "order_price", precision = 18, scale = 4)
    private BigDecimal orderPrice;

    @Column(name = "order_count", precision = 18, scale = 4)
    private BigDecimal orderCount;

    @Column(name = "remaining_count", nullable = false, precision = 18, scale = 4)
    private BigDecimal remainingCount;

    @CreationTimestamp
    @Column(name = "order_time")
    private LocalDateTime ordersTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 10)
    private OrderStatus orderStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;


    @Builder
    public Order(BigDecimal orderPrice, BigDecimal orderCount, BigDecimal remainingCount,
                 OrderType orderType, OrderStatus orderStatus, Member member, Category category) {
        this.orderPrice = orderPrice;
        this.orderCount = orderCount;
        this.remainingCount = (remainingCount != null) ? remainingCount : orderCount;
        this.orderType = orderType;
        this.orderStatus = (orderStatus != null) ? orderStatus : OrderStatus.OPEN;
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



}
