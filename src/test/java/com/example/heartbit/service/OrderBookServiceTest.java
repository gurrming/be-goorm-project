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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderBookServiceTest {

    @InjectMocks
    private OrderBookService orderBookService;

    @Mock
    private TradeEngineService tradeEngineService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @DisplayName("엔진에 데이터가 없으면 브로드캐스트를 수행하지 않는다.")
    @Test
    void broadcastOrderBookEngineEmpty() {
        // given
        Long categoryId = 999L;
        when(tradeEngineService.getMatchingOrder(categoryId)).thenReturn(null);

        // when
        orderBookService.broadcastOrderBook(categoryId);

        // then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @DisplayName("가격이 백의 자리 수 이상이면 정수로 나타낸다.")
    @Test
    void formatPriceHundred() {
        // given
        Long categoryId = 1L;
        TradeEngineService.MatchingOrder mockMatchingOrder = mock(TradeEngineService.MatchingOrder.class);

        // 가격이 123.456 -> 서비스 로직을 거쳐 123이 되었다고 가정
        List<OrderBookResponse> sellList = List.of(new OrderBookResponse(new BigDecimal("123"), new BigDecimal("1")));

        when(tradeEngineService.getMatchingOrder(categoryId)).thenReturn(mockMatchingOrder);
        when(mockMatchingOrder.getSnapshot(eq(OrderType.SELL), anyInt())).thenReturn(sellList);
        when(mockMatchingOrder.getSnapshot(eq(OrderType.BUY), anyInt())).thenReturn(List.of());

        // when
        orderBookService.broadcastOrderBook(categoryId);

        // then
        Map<String, Object> payload = getPayload();
        List<OrderBookResponse> sellOrderBook = (List<OrderBookResponse>) payload.get("sellSide");

        assertThat(sellOrderBook).hasSize(1)
                .extracting("orderPrice")
                .containsExactly(new BigDecimal("123"));
    }

    @DisplayName("가격이 십의 자리 수이면 소수점 1자리까지 나타낸다.")
    @Test
    void formatPriceTen() {
        // given
        Long categoryId = 1L;
        TradeEngineService.MatchingOrder mockMatchingOrder = mock(TradeEngineService.MatchingOrder.class);

        List<OrderBookResponse> sellList = List.of(new OrderBookResponse(new BigDecimal("12.34"), new BigDecimal("1")));

        when(tradeEngineService.getMatchingOrder(categoryId)).thenReturn(mockMatchingOrder);
        when(mockMatchingOrder.getSnapshot(eq(OrderType.SELL), anyInt())).thenReturn(sellList);
        when(mockMatchingOrder.getSnapshot(eq(OrderType.BUY), anyInt())).thenReturn(List.of());

        // when
        orderBookService.broadcastOrderBook(categoryId);

        // then
        Map<String, Object> payload = getPayload();
        List<OrderBookResponse> sellOrderBook = (List<OrderBookResponse>) payload.get("sellSide");

        assertThat(sellOrderBook).hasSize(1)
                .extracting("orderPrice", "totalRemainingCount")
                .containsExactly(tuple(new BigDecimal("12.3"), new BigDecimal("1")));
    }

    @DisplayName("가격이 일의 자리 수이면 소수점 2자리까지 나타낸다.")
    @Test
    void formatPriceOne() {
        // given
        Long categoryId = 1L;
        TradeEngineService.MatchingOrder mockMatchingOrder = mock(TradeEngineService.MatchingOrder.class);

        List<OrderBookResponse> buyList = List.of(new OrderBookResponse(new BigDecimal("1.234"), new BigDecimal("1")));

        // stubbing
        when(tradeEngineService.getMatchingOrder(categoryId)).thenReturn(mockMatchingOrder);
        when(mockMatchingOrder.getSnapshot(eq(OrderType.BUY), anyInt())).thenReturn(buyList);
        when(mockMatchingOrder.getSnapshot(eq(OrderType.SELL), anyInt())).thenReturn(List.of());

        // when
        orderBookService.broadcastOrderBook(categoryId);

        // then
        Map<String, Object> payload = getPayload();
        List<OrderBookResponse> buyOrderBook = (List<OrderBookResponse>) payload.get("buySide");

        assertThat(buyOrderBook).hasSize(1)
                .extracting("orderPrice", "totalRemainingCount")
                .containsExactly(tuple(new BigDecimal("1.23"), new BigDecimal("1")));
    }

    private Map<String, Object> getPayload() {
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        // times(1)을 명시하여 정확히 해당 테스트 메서드 내에서 1번 호출되었는지 검증
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), (Object) captor.capture());
        return (Map<String, Object>) captor.getValue();
    }
}