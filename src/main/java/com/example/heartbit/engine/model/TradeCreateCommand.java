package com.example.heartbit.engine.model;

import com.example.heartbit.dto.TradeResponse;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class TradeCreateCommand extends TradeResponse {
    private Long buyOrderId;
    private Long sellOrderId;
    private BigDecimal price;
    private BigDecimal quantity;

    private Long categoryId;
    private String takerType;
    private LocalDateTime tradeTime;


    @Override
    public BigDecimal getTradePrice() { return this.price; }

    @Override
    public BigDecimal getTradeCount() { return this.quantity; }

    @Override
    public Long getCategoryId() { return this.categoryId; }

    @Override
    public String getTakerType() { return this.takerType; }

    @Override
    public LocalDateTime getTradeTime() {
        return this.tradeTime;
    }

    public static TradeCreateCommand from(MatchResult result) {
        TradeCreateCommand cmd = new TradeCreateCommand();
        cmd.setBuyOrderId(result.getBuyOrderId());
        cmd.setSellOrderId(result.getSellOrderId());
        cmd.setPrice(result.getPrice());
        cmd.setQuantity(result.getOrderCount());
        return cmd;
    }
}