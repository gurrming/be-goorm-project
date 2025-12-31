package com.example.heartbit.domain;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "invest")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invest_id")
    private Long investId;

    @Column(name = "invest_count", precision = 18, scale = 8)
    private BigDecimal investCount;

    @Column(name = "invest_price", precision = 18, scale = 8)
    private BigDecimal investPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
