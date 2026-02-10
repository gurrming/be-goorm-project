package com.example.heartbit.disruptor;


import com.example.heartbit.domain.OrderType;
import com.example.heartbit.engine.model.MatchResult;
import com.example.heartbit.engine.model.OrderCommand;
import com.example.heartbit.engine.model.TradeCreateCommand;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter @Setter
public class OrderEvent {
    private OrderCommand command;
    private Long categoryId;
    private List<MatchResult> results;
    private List<TradeCreateCommand> tradeCommands;

    public void clear() {
        this.command = null;
        this.categoryId = null;
        this.results = null;
        this.tradeCommands = null;
    }
}





