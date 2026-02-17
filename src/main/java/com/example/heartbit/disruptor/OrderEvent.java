package com.example.heartbit.disruptor;


import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.engine.model.MatchResult;
import com.example.heartbit.engine.model.OrderCommand;
import com.example.heartbit.engine.model.TradeCreateCommand;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@NoArgsConstructor
@Getter @Setter
public class OrderEvent {


    public enum EventType { ORDER, SNAPSHOT }
    private EventType eventType;

    private Order order;
    private OrderCommand command;
    private Long categoryId;
    private OrderType type;

    private List<MatchResult> results;
    private List<TradeCreateCommand> tradeCommands;

    private CompletableFuture<List<OrderBookResponse>> snapshotFuture;
    private int limit;
    private List<OrderBookResponse> buySnapshot;
    private List<OrderBookResponse> sellSnapshot;

    public void clear() {
        this.order = null;
        this.command = null;
        this.categoryId = null;
        this.type = null;
        this.results = null;
        this.tradeCommands = null;
        this.snapshotFuture = null;
        this.limit = 0;
        this.buySnapshot = null;
        this.sellSnapshot = null;
    }
}





