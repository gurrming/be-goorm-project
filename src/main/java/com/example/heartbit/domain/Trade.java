package com.example.heartbit.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private LocalDateTime tradeTime;

    @Column(name = "trade_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal tradePrice;

    @Column(name = "trade_count", nullable = false, precision = 18, scale = 8)
    private BigDecimal tradeCount;

    @Column(name = "trade_close", precision = 18, scale = 8)
    private BigDecimal tradeClosePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_buy_id", nullable = false)
    private Order buyOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_sell_id", nullable = false)
    private Order sellOrder;


    @Builder
    public Trade (BigDecimal tradePrice, LocalDateTime tradeTime, BigDecimal tradeCount, BigDecimal tradeClosePrice, Order buyOrder, Order sellOrder ) {
        this.tradePrice = tradePrice;
        this.tradeTime = tradeTime;
        this.tradeCount = tradeCount;
        this.tradeClosePrice = tradeClosePrice;
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
        validateOrderTypes();
    }

    public void validateOrderTypes() {
        if (buyOrder == null || sellOrder == null) {
            throw new IllegalStateException("buyOrder, sellOrder 필수");
        }
        if (buyOrder.getOrderType() != OrderType.BUY) {
            throw new IllegalArgumentException("buyOrder BUY 주문");
        }
        if (sellOrder.getOrderType() != OrderType.SELL) {
            throw new IllegalArgumentException("sellOrder SELL 주문");
        }
    }

    public String getSymbol() {
        // 매수 주문이나 매도 주문 중 하나만 타고 들어가도 종목 같음
        if (this.buyOrder != null && this.buyOrder.getCategory() != null) {
            return this.buyOrder.getCategory().getSymbol();
        }
        return "UNKNOWN";
    }

}
