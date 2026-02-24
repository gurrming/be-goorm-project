package com.example.heartbit.controller;

import com.example.heartbit.dto.InvestResponse;
import com.example.heartbit.service.InvestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvestController.class)
@WithMockUser
public class InvestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvestService investService;

    @Test
    @DisplayName("멤버 ID로 투자 내역 조회 API 테스트")
    void getInvestSummaryTest() throws Exception {

        //given
        Long memberId = 1L;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        BigDecimal totalBuyAmount = new BigDecimal("1000000");


        InvestResponse response = InvestResponse.builder()
                .totalBuyAmount(totalBuyAmount)
                .build();

        given(investService.getInvestSummary(memberId, pageable))
                .willReturn(response);

        //when & then
        mockMvc.perform(get("/api/invest/summary")
                        .param("memberId", String.valueOf(memberId)) // PathVariable
                .param("page", String.valueOf(page))          // QueryParam
                .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBuyAmount").value(1000000));
    }
}
