package com.example.heartbit.engine.model;

import com.example.heartbit.domain.Category;
import com.example.heartbit.dto.TradeResponse;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class TradeCreateCommand {
    private Long buyOrderId;
    private Long sellOrderId;
    private BigDecimal price;
    private BigDecimal orderCount;

    private Long categoryId;
    private String takerType;
    private LocalDateTime tradeTime;

    public static TradeCreateCommand from(MatchResult result, Long categoryId, String takerType) {
        TradeCreateCommand cmd = new TradeCreateCommand();
        cmd.setBuyOrderId(result.getBuyOrderId());
        cmd.setSellOrderId(result.getSellOrderId());
        cmd.setPrice(result.getPrice());
        cmd.setOrderCount(result.getOrderCount());
        cmd.setCategoryId(categoryId);
        cmd.setTakerType(takerType);
        cmd.setTradeTime(LocalDateTime.now());
        return cmd;
    }
}