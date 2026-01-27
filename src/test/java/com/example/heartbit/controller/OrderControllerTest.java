package com.example.heartbit.controller;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.service.OrderService;
import com.example.heartbit.service.TradeEngineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private TradeEngineService tradeEngineService;

    @DisplayName("신규 주문을 생성한다.")
    @Test
    void createOrder() throws Exception {
        // given
        OrderRequest request = OrderRequest.builder()
                .memberId(1L)
                .categoryId(1L)
                .orderPrice(new BigDecimal("50000"))
                .orderCount(new BigDecimal("1"))
                .orderType(OrderType.BUY)
                .build();

        // when & then
        mockMvc.perform(
                        post("/api/orders")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @DisplayName("종목별 호가창을 조회한다.")
    @Test
    void orderBook() throws Exception {
        //given
        Long categoryId = 1L;
        OrderType orderType = OrderType.BUY;

        // when & then
        mockMvc.perform(
                get("/api/orders/orderbook")
                        .param("categoryId", categoryId.toString())
                        .param("orderType", orderType.name())
                        .param("limit", "30")
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"));
    }
}