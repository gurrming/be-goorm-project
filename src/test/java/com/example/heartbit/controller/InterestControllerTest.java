package com.example.heartbit.controller;

import com.example.heartbit.dto.InterestRequestDto;
import com.example.heartbit.dto.InterestResponseDto;
import com.example.heartbit.service.InterestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class InterestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InterestService interestService;

    @Test
    @WithMockUser
    @DisplayName("멤버 ID와 종목 ID를 받아 관심 종목을 추가한다.")
    void interestAdd() throws Exception {
        // given
        String jsonRequest = """
            {
                "memberId": 1,
                "categoryId": 10
            }
            """;

        InterestResponseDto response = new InterestResponseDto(1L, 1L, 10L);


        given(interestService.interestAdd(anyLong(), anyLong())).willReturn(response);

        // 2. when & then
        mockMvc.perform(post("/api/interests")
                        .with(csrf())
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interestId").value(1))
                .andExpect(jsonPath("$.categoryId").value(10));
    }

    @Test
    @WithMockUser
    @DisplayName("멤버 ID를 통해 해당 멤버의 관심 종목 목록을 조회한다.")
    void interestList() throws Exception {
        // given
        List<InterestResponseDto> responses = List.of(
                new InterestResponseDto(1L, 1L, 10L),
                new InterestResponseDto(2L, 1L, 11L)
        );

        given(interestService.getInterest(anyLong())).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/interests")
                        .param("memberId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].interestId").value(1))
                .andExpect(jsonPath("$[0].categoryId").value(10))
                .andExpect(jsonPath("$[1].interestId").value(2))
                .andExpect(jsonPath("$[1].categoryId").value(11));
    }

    @Test
    @WithMockUser
    @DisplayName("관심 종목 ID를 받아 해당 내역을 삭제한다.")
    void interestDelete() throws Exception {
        // given
        Long interestId = 1L;
        doNothing().when(interestService).delete(interestId);

        // when & then
        mockMvc.perform(delete("/api/interests/{interestId}", interestId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}