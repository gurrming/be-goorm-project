package com.example.heartbit.controller;

import com.example.heartbit.dto.trade.TradeResponse;
import com.example.heartbit.service.TradeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeController.class)
@WithMockUser
public class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeService tradeService;

    @Test
    @DisplayName("종목별로 최신 체결리스트 20개를 조회한다.")
    void getLatestTradesByCategoryTest() throws Exception {
        // given
        Long categoryId = 1L;
        int limit = 20;
        TradeResponse response = TradeResponse.builder()
                .tradePrice(new BigDecimal("50000"))
                .tradeCount(new BigDecimal("1"))
                .tradeTime(LocalDateTime.now())
                .build();

        given(tradeService.getTradeList(categoryId, limit)).willReturn(List.of(response));

        // when & then
        // @RequestParam이므로 .param()을 사용합니다.
        mockMvc.perform(get("/api/trades")
                        .param("categoryId", String.valueOf(categoryId))
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tradePrice").value(50000))
                .andDo(print());
    }

    @Test
    @DisplayName("종목별로 최신 체결 1건을 조회한다.")
    void getLatestTradeByCategoryTest() throws Exception {
        // given
        Long categoryId = 1L;
        TradeResponse response = TradeResponse.builder()
                .tradePrice(new BigDecimal("51000"))
                .build();

        given(tradeService.getRecentTrade(categoryId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/trades/recent")
                        .param("categoryId", String.valueOf(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradePrice").value(51000))
                .andDo(print());
    }

    @Test
    @DisplayName("주문 ID로 체결된 특정 내역의 주문 상태를 조회한다.")
    void getTradeByOrderIdTest() throws Exception {
        // given
        Long orderId = 100L;
        TradeResponse response = TradeResponse.builder()
                .buyOrderId(orderId)
                .tradePrice(new BigDecimal("49000"))
                .build();

        given(tradeService.getTradeByOrder(orderId)).willReturn(List.of(response));

        // when & then
        // @PathVariable이므로 URL 경로에 직접 포함합니다.
        mockMvc.perform(get("/api/trades/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tradePrice").value(49000))
                .andDo(print());
    }

    @Test
    @DisplayName("멤버 ID로 개인 체결 내역을 조회한다.")
    void getTradesByMemberIdTest() throws Exception {
        // given
        Long memberId = 1L;
        int page = 0;
        int size = 10;
        TradeResponse response = TradeResponse.builder()
                .tradePrice(new BigDecimal("52000"))
                .build();

        given(tradeService.getMyTrade(memberId, page, size)).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/trades/my")
                        .param("memberId", String.valueOf(memberId))
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tradePrice").value(52000))
                .andDo(print());
    }

    @Test
    @DisplayName("마지막 데이터 ID를 기준으로 이전 체결내역을 조회한다.")
    void getTradesByLastDataIdTest() throws Exception {
        // given
        Long categoryId = 1L;
        Long lastId = 500L;
        int size = 20;
        Map<String, Object> candle = Map.of("price", 50000, "volume", 1.5);

        given(tradeService.getInitialCandles(categoryId, lastId, size)).willReturn(List.of(candle));

        // when & then
        mockMvc.perform(get("/api/trades/chart")
                        .param("categoryId", String.valueOf(categoryId))
                        .param("lastId", String.valueOf(lastId))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price").value(50000))
                .andDo(print());
    }
}