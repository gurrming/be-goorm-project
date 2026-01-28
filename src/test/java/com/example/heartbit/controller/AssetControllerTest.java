package com.example.heartbit.controller;

import com.example.heartbit.dto.AssetResponse;
import com.example.heartbit.service.AssetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@WithMockUser
public class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetService assetService;

    @Test
    @DisplayName("개인 자산 내역 조회 API 테스트")
    void getMyAssetTest() throws Exception {

        //given
        Long memberId = 1L;
        BigDecimal expectedAssetCash = new BigDecimal("500000000");

        AssetResponse response = AssetResponse.builder()
                .assetCash(expectedAssetCash)
                .build();

        given(assetService.getAssetByMemberId(1L))
                .willReturn(response);


        //when & then
        mockMvc.perform(get("/api/assets/{memberId}", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetCash").value(500000000));

    }
}
