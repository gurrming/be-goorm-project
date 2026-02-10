package com.example.heartbit.engine.core;

import com.example.heartbit.domain.OrderType;
import com.example.heartbit.engine.model.MatchResult;
import com.example.heartbit.engine.model.OrderCommand;
import java.math.BigDecimal;
import java.util.*;

public class MatchingEngine {

    public List<MatchResult> match(OrderBook book, OrderCommand taker) {
        List<MatchResult> results = new ArrayList<>();

        NavigableMap<BigDecimal, Queue<OrderCommand>> opposite = book.opposite(taker.getType());

        while (taker.getRemainingCount().signum() > 0 && !opposite.isEmpty()) {
            Map.Entry<BigDecimal, Queue<OrderCommand>> bestEntry = opposite.firstEntry();
            BigDecimal price = bestEntry.getKey();

            if (!isMatchable(taker, price)) break;

            Queue<OrderCommand> makers = bestEntry.getValue();
            OrderCommand maker = makers.peek();

            if (maker == null) {
                opposite.remove(price);
                continue;
            }

            BigDecimal tradeCount = taker.getRemainingCount().min(maker.getRemainingCount());
            taker.reduce(tradeCount);
            maker.reduce(tradeCount);

            results.add(new MatchResult(
                    taker.getType() == OrderType.BUY ? taker.getOrderId() : maker.getOrderId(),
                    taker.getType() == OrderType.SELL ? taker.getOrderId() : maker.getOrderId(),
                    price, tradeCount
            ));

            if (maker.getRemainingCount().signum() <= 0) {
                makers.poll();
            }
            if (makers.isEmpty()) {
                opposite.remove(price);
            }
        }

        if (taker.getRemainingCount().signum() > 0) {
            book.add(taker);
        }
        return results;
    }

    private boolean isMatchable(OrderCommand order, BigDecimal price) {
        return order.getType() == OrderType.BUY
                ? order.getOrderPrice().compareTo(price) >= 0
                : order.getOrderPrice().compareTo(price) <= 0;
    }
}