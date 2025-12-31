package com.example.heartbit.service;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderStatus;
import com.example.heartbit.dto.OrderRequest;
import com.example.heartbit.dto.OrderResponse;
import com.example.heartbit.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse createOrder(@Valid OrderRequest request) {

        return OrderResponse.builder()
                .orderId(1L)
                .orderStatus(OrderStatus.OPEN)
                .build();
    }

    public List<OrderResponse> getOrderByMember(Long memberId) {
        return orderRepository.findByMember_MemberId(memberId).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelAllOrders(Long memberId) {
        List<Order> orders = orderRepository.findByMember_MemberId(memberId);
        orders.forEach(order -> {
            if (order.getOrderStatus() == OrderStatus.OPEN) {
                //상태를 cancelled로 변경해줘야하는 로직 구현해야함.
            }
        });
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        //상태를 cancelled로 변경해줘야하는 로직 구현해야함.
    }


}
