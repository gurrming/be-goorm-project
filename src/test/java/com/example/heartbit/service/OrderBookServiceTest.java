package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderStatus;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderBookResponse;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class OrderBookServiceTest {

    @Autowired
    private OrderBookService orderBookService;

    @Autowired
    private TradeEngineService tradeEngineService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @AfterEach
    void tearDown() {
        tradeEngineService.deleteAllOrders();
    }

    @DisplayName("엔진에 데이터가 없으면 브로드캐스트를 수행하지 않는다.")
    @Test
    void broadcastOrderBookEngineEmpty() {
        // given
        Long categoryId = 999L;

        // when
        orderBookService.broadcastOrderBook(categoryId);

        // then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @DisplayName("가격이 일의 자리 수이면 소수점 2자리까지 버림 처리한다.")
    @Test
    void formatPriceOne() {
        // given
        Category category = createCategory();
        Order orderPrice = createOrder(category, OrderType.BUY, "1.234", "1");
        tradeEngineService.processOrder(orderPrice);

        // when
        orderBookService.broadcastOrderBook(category.getCategoryId());

        // then
        Map<String, Object> payload = payloads();
        List<OrderBookResponse> buyOrderBook = (List<OrderBookResponse>) payload.get("buySide");

        assertThat(buyOrderBook).hasSize(1)
                .extracting("orderPrice", "totalRemainingCount")
                .containsExactly(tuple(new BigDecimal("1.23"), new BigDecimal("1")));
    }

    @DisplayName("가격이 십의 자리 수이면 소수점 1자리까지 버림 처리한다.")
    @Test
    void formatPriceTen() {
        // given
        Category category = createCategory();
        Order orderPrice = createOrder(category, OrderType.SELL, "12.34", "1");
        tradeEngineService.processOrder(orderPrice);

        // when
        orderBookService.broadcastOrderBook(category.getCategoryId());

        // then
        Map<String, Object> payload = payloads();
        List<OrderBookResponse> sellOrderBook = (List<OrderBookResponse>) payload.get("sellSide");

        assertThat(sellOrderBook).hasSize(1)
                .extracting("orderPrice", "totalRemainingCount")
                .containsExactly(tuple(new BigDecimal("12.3"), new BigDecimal("1")));
    }

    @DisplayName("가격이 100 이상이면 소수점을 모두 버린다.")
    @Test
    void formatPriceHundred() {
        // given
        Category category = createCategory();
        Order orderPrice = createOrder(category, OrderType.SELL, "123.4", "1");
        tradeEngineService.processOrder(orderPrice);

        // when
        orderBookService.broadcastOrderBook(category.getCategoryId());

        // then
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(anyString(), payloadCaptor.capture());

        Map<String, Object> payload = payloads();
        List<OrderBookResponse> sellOrderBook = (List<OrderBookResponse>) payload.get("sellSide");

        assertThat(sellOrderBook).hasSize(1)
                .extracting("orderPrice", "totalRemainingCount")
                .containsExactly(tuple(new BigDecimal("123"), new BigDecimal("1")));
    }

    private Category createCategory() {
        return Category.builder()
                .categoryId(1L)
                .symbol("BTC")
                .build();
    }

    private Order createOrder(Category category, OrderType type, String price, String count) {
        return Order.builder()
                .orderId(1L)
                .category(category)
                .orderType(type)
                .orderPrice(new BigDecimal(price))
                .orderCount(new BigDecimal(count))
                .remainingCount(new BigDecimal(count))
                .orderStatus(OrderStatus.OPEN)
                .build();
    }

    private Map<String, Object> payloads() {
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(anyString(), (Object) captor.capture());
        return (Map<String, Object>) captor.getValue();
    }
}