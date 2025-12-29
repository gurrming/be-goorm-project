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
    @Column(name = "orders_id")
    private Long ordersId;

    @Column(name = "orders_price", precision = 18, scale = 4)
    private BigDecimal ordersPrice;

    @Column(name = "orders_count", precision = 18, scale = 4)
    private BigDecimal ordersCount;

    @Column(name = "remaining_count", nullable = false, precision = 18, scale = 4)
    private BigDecimal remainingCount;

    @CreationTimestamp
    @Column(name = "orders_time")
    private LocalDateTime ordersTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "orders_type", nullable = false, length = 10)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "orders_status", nullable = false, length = 10)
    private OrderStatus orderStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;


    @Builder
    public Order(BigDecimal ordersPrice, BigDecimal ordersCount, BigDecimal remainingCount,
                 OrderType orderType, OrderStatus orderStatus, Member member, Category category) {
        this.ordersPrice = ordersPrice;
        this.ordersCount = ordersCount;
        this.remainingCount = (remainingCount != null) ? remainingCount : ordersCount;
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
