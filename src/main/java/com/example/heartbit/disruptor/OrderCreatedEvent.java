package com.example.heartbit.disruptor;

import com.example.heartbit.domain.Order;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderCreatedEvent {
    private final Order order;
}
