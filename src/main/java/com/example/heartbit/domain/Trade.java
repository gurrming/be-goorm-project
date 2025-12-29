package com.example.heartbit.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "trade")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tradeId;

    @CreationTimestamp
    @Column(name = "trade_time",  nullable = false, updatable = false)
    private Timestamp tradeTime;

    @Column(name = "trade_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal tradePrice;

    @Column(name = "trade_count", nullable = false, precision = 18, scale = 4)
    private BigDecimal tradeCount;

    @Column(name = "trade_close", precision = 18, scale = 4)
    private BigDecimal tradeClosePrice;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_buy_id", nullable = false)
    private Order buyOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_sell_id", nullable = false)
    private Order sellOrder;

    @Builder
    public Trade (Timestamp tradeTime, BigDecimal tradePrice, BigDecimal tradeCount, BigDecimal tradeClosePrice, Order buyOrder, Order sellOrder ) {
        this.tradeTime = tradeTime;
        this.tradePrice = tradePrice;
        this.tradeCount = tradeCount;
        this.tradeClosePrice = tradeClosePrice;
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
    }


}
