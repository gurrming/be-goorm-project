package com.example.heartbit.controller;

import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.service.OrderService;
import com.example.heartbit.service.TradeEngineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private TradeEngineService tradeEngineService;

    @DisplayName("신규 주문을 생성한다.")
    @Test
    void createOrder() throws Exception {
        // given
        String jsonRequest = """
            {
                "memberId": 1,
                "categoryId": 1,
                "orderPrice": 50000,
                "orderCount": 1,
                "orderType": "BUY"
            }
            """;

        // when & then
        mockMvc.perform(
                    post("/api/orders")
                            .content(jsonRequest)
                            .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                )
                .andDo(print())
                .andExpect(status().isOk());
    }

    @DisplayName("종목별 호가창을 조회해 메모리에 저장되어있는 스냅샷을 반환한다.")
    @Test
    void orderBook() throws Exception {
        // given
        Long categoryId = 1L;
        OrderType orderType = OrderType.BUY;
        int limit = 30;

        List<OrderBookResponse> mockResponses = List.of(
                new OrderBookResponse(new BigDecimal("15000"), new BigDecimal("100")),
                new OrderBookResponse(new BigDecimal("14900"), new BigDecimal("50"))
        );

        // Mocking 설정
        TradeEngineService.MatchingOrder mockEngine = mock(TradeEngineService.MatchingOrder.class);
        given(tradeEngineService.getMatchingOrder(categoryId)).willReturn(mockEngine);
        given(mockEngine.getSnapshot(orderType, limit)).willReturn(mockResponses);

        // When & Then
        mockMvc.perform(
                        get("/api/orders/orderbook")
                                .param("categoryId", String.valueOf(categoryId))
                                .param("orderType", orderType.name())
                                .param("limit", String.valueOf(limit))
                                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].orderPrice").value(15000))
                .andExpect(jsonPath("$[0].totalRemainingCount").value(100))
                .andExpect(jsonPath("$[1].orderPrice").value(14900))
                .andExpect(jsonPath("$[1].totalRemainingCount").value(50));
    }

    @DisplayName("회원의 미체결 주문 내역을 조회한다.")
    @Test
    void getOpenMyOrders() throws Exception {
        // when & then
        mockMvc.perform(
                    get("/api/orders/open")
                            .param("memberId", "1")
                            .param("page", "0")
                            .param("size", "10")
                )
                .andDo(print())
                .andExpect(status().isOk());
    }

    @DisplayName("특정 주문을 취소한다.")
    @Test
    void cancelOrder() throws Exception {
        // when & then
        mockMvc.perform(
                    patch("/api/orders/{orderId}/cancel", 100L)
                )
                .andDo(print())
                .andExpect(status().isNoContent()); // ResponseEntity.noContent() 대응
    }

    @DisplayName("회원의 모든 주문을 일괄 취소한다.")
    @Test
    void cancelAllOrders() throws Exception {
        // when & then
        mockMvc.perform(
                    patch("/api/orders/cancel-all")
                            .param("memberId", "1")
                )
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}

