package com.example.heartbit.acceptance.asset;

import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.AssetResponse;
import com.example.heartbit.repository.MemberRepository;
import com.example.heartbit.service.AssetService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class AssetAcceptanceTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssetService assetService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("사용자는 자신의 자산 총액과 현금 잔고를 확인할 수 있다.")
    void getMyAssetTest() throws Exception {
        //given
        Member member = memberRepository.save(Member.builder()
                .memberEmail("test5@gmail.com")
                .memberNickname("qqqqqqqq")
                .build());
        assetService.createInitialAsset(member);

        // when & then: 실제 API를 호출하고 200 응답과 데이터를 확인
        mockMvc.perform(get("/api/assets/" + member.getMemberId()) // 실제 API 경로
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk()) // ★ 200 OK 응답 확인 ★
                .andExpect(jsonPath("$.assetCash").value(500000000)) // 응답 JSON 데이터 확인
                .andExpect(jsonPath("$.totalAsset").value(500000000));
    }
}
