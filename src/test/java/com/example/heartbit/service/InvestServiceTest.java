package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Invest;
import com.example.heartbit.dto.InvestResponse;
import com.example.heartbit.dto.trade.PriceChangedEvent;
import com.example.heartbit.dto.trade.TradeResponse;
import com.example.heartbit.repository.InvestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestServiceTest {

    @InjectMocks
    private InvestService investService;

    @Mock
    private InvestRepository investRepository;
    @Mock
    private TradeService tradeService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("추가 매수 시 평단가가 정확하게 계산되어야 한다 (물타기)")
    void updateInvestPriceOnAdditionalBuy() {
        // given: 기존 100원에 10개 보유 (총 1000원)
        Category category = Category.builder().categoryId(1L).build();
        Invest existingInvest = Invest.builder()
                .investId(1L)
                .investCount(new BigDecimal("10"))
                .investPrice(new BigDecimal("100"))
                .category(category)
                .build();

        when(investRepository.findByMember_MemberIdAndCategory_CategoryId(1L, 1L))
                .thenReturn(Optional.of(existingInvest));

        // when: 200원에 10개 추가 매수 (총 2000원 추가)
        // 새로운 평단가 예상: (1000 + 2000) / 20 = 150원
        investService.saveOrUpdateInvest(1L, null, 1L, new BigDecimal("10"), new BigDecimal("200"), "BUY");

        // then
        assertThat(existingInvest.getInvestCount()).isEqualByComparingTo("20");
        assertThat(existingInvest.getInvestPrice()).isEqualByComparingTo("150");
        verify(investRepository).save(existingInvest);
    }

    @Test
    @DisplayName("매도 시 수량은 줄어들지만 평단가는 변하지 않아야 한다")
    void updateInvestCountOnSell() {
        // given: 100원에 10개 보유
        Invest existingInvest = Invest.builder()
                .investCount(new BigDecimal("10"))
                .investPrice(new BigDecimal("100"))
                .build();
        when(investRepository.findByMember_MemberIdAndCategory_CategoryId(1L, 1L))
                .thenReturn(Optional.of(existingInvest));

        // when: 5개 매도
        investService.saveOrUpdateInvest(1L, null, 1L, new BigDecimal("5"), new BigDecimal("500"), "SELL");

        // then
        assertThat(existingInvest.getInvestCount()).isEqualByComparingTo("5");
        assertThat(existingInvest.getInvestPrice()).isEqualByComparingTo("100"); // 평단가 불변
    }

    @Test
    @DisplayName("전량 매도하여 수량이 0이 되면 데이터가 삭제되어야 한다")
    void deleteInvestWhenCountIsZero() {
        // given
        Invest existingInvest = Invest.builder()
                .investId(1L)
                .investCount(new BigDecimal("10"))
                .build();
        when(investRepository.findByMember_MemberIdAndCategory_CategoryId(1L, 1L))
                .thenReturn(Optional.of(existingInvest));

        // when: 10개 전량 매도
        investService.saveOrUpdateInvest(1L, null, 1L, new BigDecimal("10"), new BigDecimal("100"), "SELL");

        // then
        verify(investRepository).delete(existingInvest);
    }

    @Test
    @DisplayName("자산 요약 조회 시 전체 합계와 수익률이 정확해야 한다")
    void getInvestSummaryCalculation() {
        Long memberId = 1L;
        Pageable pageable = Pageable.unpaged(); // 또는 PageRequest.of(0, 10)
        // given: A코인(평단 100, 10개), 현재가 150원 가정
        Category category = Category.builder().categoryId(1L).categoryName("A코인").build();
        Invest invest = Invest.builder()
                .category(category)
                .investCount(new BigDecimal("10"))
                .investPrice(new BigDecimal("100")) // 매수금 1000
                .build();

        // [추가] Slice 객체 생성 (Spring Data의 SliceImpl 사용)
        Slice<Invest> pagedInvests = new SliceImpl<>(List.of(invest));

        // [기존] 전체 리스트 반환 설정
        when(investRepository.findAllByMember_MemberId(memberId)).thenReturn(List.of(invest));

        // [수정/추가] 페이징(Slice) 반환 설정 - 이 부분이 누락되어 NPE가 발생함
        when(investRepository.findAllByMember_MemberId(memberId, pageable)).thenReturn(pagedInvests);

        when(tradeService.getCurrentTrade(1L)).thenReturn(TradeResponse.builder().tradePrice(new BigDecimal("150")).build());

        // when
        InvestResponse response = investService.getInvestSummary(memberId, pageable);

        // then
        assertThat(response.getTotalBuyAmount()).isEqualByComparingTo("1000");
        assertThat(response.getTotalEvaluation()).isEqualByComparingTo("1500");
        assertThat(response.getTotalProfit()).isEqualByComparingTo("500");
        assertThat(response.getTotalProfitRate()).isEqualByComparingTo("50"); // 50% 수익률
    }


    @Test
    @DisplayName("이벤트 수신 시 자산 브로드캐스팅 로직이 실행되어야 한다.")
    void handlePriceChangeTriggerBroadcast() {

        //given
        Long categoryId = 1L;
        BigDecimal newPrice = new BigDecimal("200");
        PriceChangedEvent event = new PriceChangedEvent(categoryId, newPrice);

        given(investRepository.findMemberIdsByCategoryId(categoryId)).willReturn(List.of(1L));
        given(investRepository.findAllByMember_MemberId(any(Long.class))).willReturn(List.of());
        given(investRepository.findAllByMember_MemberId(any(Long.class), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of()));


        // when
        investService.handlePriceChange(event);

        //then
        then(messagingTemplate).should().convertAndSend(eq("/topic/invest/1"), any(InvestResponse.class));
    }

    @Test
    @DisplayName("전송 중 예외가 발생해도 catch 블록에서 처리되고, 다음 유저에게 계속 전송되어야 한다")
    void verifyCatchBlockBehavior() {
        // given
        Long categoryId = 1L;
        Long errorMemberId = 1L;   // 에러가 날 유저
        Long successMemberId = 2L; // 정상 전송될 유저

        given(investRepository.findMemberIdsByCategoryId(categoryId))
                .willReturn(List.of(errorMemberId, successMemberId));

        given(investRepository.findAllByMember_MemberId(any(Long.class)))
                .willReturn(List.of());
        given(investRepository.findAllByMember_MemberId(any(Long.class), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of()));

        willThrow(new RuntimeException("의도된 전송 실패"))
                .given(messagingTemplate)
                .convertAndSend(eq("/topic/invest/" + errorMemberId), any(Object.class));

        // when
        investService.broadcastAssetUpdate(categoryId, new BigDecimal("100"));

        // then
        then(messagingTemplate).should().convertAndSend(eq("/topic/invest/" + errorMemberId), any(Object.class));

        then(messagingTemplate).should().convertAndSend(eq("/topic/invest/" + successMemberId), any(Object.class));
    }
}