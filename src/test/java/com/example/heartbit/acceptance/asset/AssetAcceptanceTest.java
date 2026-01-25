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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class AssetAcceptanceTest {

    @Autowired
    private AssetService assetService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("사용자는 자신의 자산 총액과 현금 잔고를 확인할 수 있다.")
    void getMyAssetTest() {
        //given
        Member member = memberRepository.save(Member.builder()
                .memberEmail("test6@gmail.com")
                .memberNickname("qqqqqqqq")
                .build());
        assetService.createInitialAsset(member);

        //when
        AssetResponse response = assetService.getAssetByMemberId(member.getMemberId());

        //then
        assertThat(response.getAssetCash()).isEqualByComparingTo("500000000");
        assertThat(response.getTotalAsset()).isEqualByComparingTo("500000000");

    }
}
