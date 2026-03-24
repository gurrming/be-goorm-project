package com.example.heartbit.service.trade;


import com.example.heartbit.service.trade.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TradeInitializer {

    private final TradeService tradeService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void initData() {
        tradeService.init();
    }
}