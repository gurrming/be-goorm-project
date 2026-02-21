//package com.example.heartbit.service;
//
//import com.example.heartbit.domain.Asset;
//import com.example.heartbit.domain.Invest;
//import com.example.heartbit.domain.Member;
//import com.example.heartbit.dto.AssetResponse;
//import com.example.heartbit.repository.AssetRepository;
//import com.example.heartbit.repository.InvestRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.then;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AssetServiceTest {
//
//    @InjectMocks
//    private AssetService assetService;
//
//    @Mock
//    private AssetRepository assetRepository;
//
//    @Mock
//    private InvestRepository investRepository;
//
//    @Mock
//    private Member member;
//
//    @Mock
//    private SimpMessagingTemplate messagingTemplate;
//
//    @Test
//    @DisplayName("초기 자산 생성 테스트 - 5억원이 정상적으로 입금되어야 한다")
//    void createInitialAssetTest() {
//        // given
//        // when
//        assetService.createInitialAsset(member);
//
//        // then
//        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
//        verify(assetRepository).save(assetCaptor.capture());
//
//        Asset savedAsset = assetCaptor.getValue();
//        assertThat(savedAsset.getAssetCash()).isEqualByComparingTo("500000000");
//        assertThat(savedAsset.getAssetCanOrder()).isEqualByComparingTo("500000000");
//    }
//
//    @Test
//    @DisplayName("자산 조회 시 투자 내역의 합계가 정상적으로 계산되어야 한다.")
//    void getAssetByMemberIdTest() {
//
//        //given
//        Long memberId = 1L;
//        Asset asset = Asset.builder()
//                .assetCash(new BigDecimal("1000"))
//                .assetCanOrder(new BigDecimal("1000"))
//                .build();
//
//        Invest invest = Mockito.mock(Invest.class);
//        given(invest.getInvestCount()).willReturn(new BigDecimal("10"));
//        given(invest.getInvestPrice()).willReturn(new BigDecimal("100"));
//
//        given(assetRepository.findByMember_MemberId(memberId)).willReturn(Optional.of(asset));
//        given(investRepository.findAllByMember_MemberId(memberId)).willReturn(java.util.List.of(invest));
//
//        //when
//        AssetResponse response = assetService.getAssetByMemberId(memberId);
//
//        //then
//        assertThat(response.getTotalAsset()).isEqualByComparingTo("2000");
//    }
//
//    @Test
//    @DisplayName("자산 조회 실패 시 IllegalArgumentException 예외가 발생한다")
//    void getAssetByMemberIdFailTest() {
//        //given
//        Long memberId = 1L;
//        given(assetRepository.findByMember_MemberId(memberId)).willReturn(Optional.empty());
//
//        //when & then
//        assertThatThrownBy(() -> assetService.getAssetByMemberId(memberId))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("해당 회원의 자산 정보를 찾을 수 없습니다.");
//    }
//
//
//    @Test
//    @DisplayName("스케줄러 실행 시 모든 자산 정보를 웹소켓으로 전송해야 한다.")
//    void sendAssetUpdateTest() {
//
//        //given
//        Long memberId = 1L;
//        Member testMember = Member.builder().memberId(memberId).build();
//        Asset asset = Asset.builder().member(testMember).build();
//
//        given(assetRepository.findAll()).willReturn(List.of(asset));
//
//        given(assetRepository.findByMember_MemberId(memberId)).willReturn(Optional.of(asset));
//        given(investRepository.findAllByMember_MemberId(memberId)).willReturn(List.of());
//
//        //when
//        assetService.sendAssetUpdate();
//
//        //then
//        then(messagingTemplate).should().convertAndSend(eq("/topic/asset/" + memberId), any(AssetResponse.class));
//
//    }
//
//    @Test
//    @DisplayName("자산 업데이트 전송 중 예외가 발생해도 로그를 남기고 다음 루프를 돌아야한다")
//    void sendAssetUpdate_Fail_Log_Exception() {
//
//        //given
//        Member member = Member.builder().memberId(1L).build();
//        Asset asset = Asset.builder().member(member).build();
//
//        given(assetRepository.findAll()).willReturn(List.of(asset));
//        given(assetRepository.findByMember_MemberId(1L)).willThrow(new RuntimeException("DB 오류"));
//
//        //when
//        assetService.sendAssetUpdate();
//
//        //then
//        then(messagingTemplate).should(never()).convertAndSend(anyString(), any(AssetResponse.class));
//
//
//    }
//
//    @Test
//    @DisplayName("매수 주문 시 주문 가능 금액 차감 테스트")
//    void blockCashTest() {
//        // given
//        Asset asset = Asset.builder()
//                .assetCash(new BigDecimal("1000"))
//                .assetCanOrder(new BigDecimal("1000"))
//                .build();
//        when(assetRepository.findByMember_MemberId(1L)).thenReturn(Optional.of(asset));
//
//        // when
//        assetService.blockCash(1L, new BigDecimal("300"));
//
//        // then
//        assertThat(asset.getAssetCanOrder()).isEqualByComparingTo("700");
//        assertThat(asset.getAssetCash()).isEqualByComparingTo("1000");
//    }
//
//    @Test
//    @DisplayName("주문 차감 시 자산 정보를 찾을 수 없으면 예외가 발생한다")
//    void blockCash_Fail_NotFound() {
//        // given
//        Long memberId = 99L;
//        given(assetRepository.findByMember_MemberId(memberId)).willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> assetService.blockCash(memberId, new BigDecimal("1000")))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("자산을 찾을 수 없습니다.");
//    }
//
//
//    @Test
//    @DisplayName("매수 체결 시 현금 차감 및 차액 복구 테스트 (방법 A 적용)")
//    void settleBuyTradeTest() {
//        // given
//        Asset asset = Asset.builder()
//                .assetCash(new BigDecimal("1000"))
//                .assetCanOrder(new BigDecimal("700"))
//                .build();
//        when(assetRepository.findByMember_MemberId(1L)).thenReturn(Optional.of(asset));
//
//        // when: 300원을 막았는데 실제로는 250원만 체결됨
//        assetService.settleBuyTrade(1L, new BigDecimal("250"), new BigDecimal("300"));
//
//        // then
//        assertThat(asset.getAssetCash()).isEqualByComparingTo("750"); // 1000 - 250
//        assertThat(asset.getAssetCanOrder()).isEqualByComparingTo("750"); // 700 + (300 - 250)
//    }
//
//    @Test
//    @DisplayName("매수 정산 시 자산 정보를 찾을 수 없으면 예외가 발생한다")
//    void settleBuyTrade_Fail_NotFound() {
//        // given
//        Long memberId = 99L;
//        given(assetRepository.findByMember_MemberId(memberId)).willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() ->
//                assetService.settleBuyTrade(memberId, BigDecimal.TEN, BigDecimal.TEN)
//        ).isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("자산을 찾을 수 없습니다.");
//    }
//
//
//    @Test
//    @DisplayName("매도 체결 시 자산 증가 테스트")
//    void settleSellTradeTest() {
//        // given
//        Asset asset = Asset.builder()
//                .assetCash(new BigDecimal("1000"))
//                .assetCanOrder(new BigDecimal("1000"))
//                .build();
//        when(assetRepository.findByMember_MemberId(1L)).thenReturn(Optional.of(asset));
//
//        // when: 500원어치 코인 매도 체결
//        assetService.settleSellTrade(1L, new BigDecimal("500"));
//
//        // then
//        assertThat(asset.getAssetCash()).isEqualByComparingTo("1500");
//        assertThat(asset.getAssetCanOrder()).isEqualByComparingTo("1500");
//    }
//
//
//    @Test
//    @DisplayName("매도 정산 시 자산 정보를 찾을 수 없으면 예외가 발생한다")
//    void settleSellTrade_Fail_NotFound() {
//        // given
//        Long memberId = 99L;
//        // BDDMockito: 자산이 없는 상황(empty)을 가정
//        given(assetRepository.findByMember_MemberId(memberId)).willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> assetService.settleSellTrade(memberId, new BigDecimal("500")))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("자산을 찾을 수 없습니다.");
//    }
//
//
//
//    @Test
//    @DisplayName("주문 취소 시 주문가능금액이 복구되어야 한다.")
//    void restoreCashTest() {
//
//        //given
//        Asset asset = Asset.builder()
//                .assetCanOrder(new BigDecimal("700"))
//                .build();
//
//        given(assetRepository.findByMember_MemberId(1L)).willReturn(Optional.of(asset));
//
//        //when
//        assetService.restoreCash(1L, new BigDecimal("300"));
//
//        //then
//        assertThat(asset.getAssetCanOrder()).isEqualByComparingTo("1000");
//    }
//
//
//    @Test
//    @DisplayName("주문 취소 복구 시 자산 정보를 찾을 수 없으면 예외가 발생한다")
//    void restoreCash_Fail_NotFound() {
//        // given
//        Long memberId = 99L;
//        given(assetRepository.findByMember_MemberId(memberId)).willReturn(Optional.ofNullable(null)); // Optional.empty()와 동일
//
//        // when & then
//        assertThatThrownBy(() -> assetService.restoreCash(memberId, new BigDecimal("300")))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("자산을 찾을 수 없습니다.");
//    }
//
//
//}