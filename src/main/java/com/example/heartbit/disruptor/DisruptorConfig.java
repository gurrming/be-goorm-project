package com.example.heartbit.disruptor;

import com.example.heartbit.disruptor.handler.MatchingHandler;
import com.example.heartbit.disruptor.handler.OrderDbHandler;
import com.example.heartbit.disruptor.handler.OrderEventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadFactory;

@Configuration
public class DisruptorConfig {

    @Bean
    public Disruptor<OrderEvent> disruptor(
            MatchingHandler matchingHandler,
            OrderDbHandler dbHandler,
            OrderEventHandler eventHandler
    ) {
        OrderEventFactory factory = new OrderEventFactory();

        int bufferSize = 1024;

        ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;

        Disruptor<OrderEvent> disruptor = new Disruptor<>(
                factory,
                bufferSize,
                threadFactory
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

