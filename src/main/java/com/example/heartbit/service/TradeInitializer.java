package com.example.heartbit.service;


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
    @Transactional
    public void initData() {
        tradeService.init();
    }
}
