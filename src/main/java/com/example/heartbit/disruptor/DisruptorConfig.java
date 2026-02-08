package com.example.heartbit.disruptor;

import com.example.heartbit.disruptor.handler.MatchingHandler;
import com.example.heartbit.disruptor.handler.OrderDbHandler;
import com.example.heartbit.disruptor.handler.OrderEventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class DisruptorConfig {

    @Bean
    public Disruptor<OrderEvent> disruptor(
            MatchingHandler matchingHandler,
            OrderDbHandler dbHandler,
            OrderEventHandler eventHandler
    ) {
        int bufferSize = 1024;

        Disruptor<OrderEvent> disruptor = new Disruptor<>(
                OrderEvent::new,
                bufferSize,
                Executors.defaultThreadFactory()
        );

        disruptor.handleEventsWith(matchingHandler)
                .then(dbHandler)
                .then(eventHandler);

        disruptor.start();
        return disruptor;
    }
    @Bean
    public RingBuffer<OrderEvent> ringBuffer(Disruptor<OrderEvent> disruptor) {
        return disruptor.getRingBuffer();
    }
}

