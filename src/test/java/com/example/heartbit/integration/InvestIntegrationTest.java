package com.example.heartbit.integration;

import com.example.heartbit.domain.*;
import com.example.heartbit.repository.*;
import com.example.heartbit.service.InvestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvestIntegrationTest {

   @InjectMocks
   private InvestService investService;

   @Mock
    private InvestRepository investRepository;

   @Mock
    private MemberRepository memberRepository;

   @Mock
    private CategoryRepository categoryRepository;

   @Test
   @DisplayName("매수 시 투자 내역이 생성된다.")
   void saveNewInvestTest() {
       //given
       Long memberId = 1L;
       Long categoryId = 1L;
       BigDecimal buyCount = new BigDecimal("10");
       BigDecimal buyPrice = new BigDecimal("15000");

       given(investRepository.findByMember_MemberIdAndCategory_CategoryId(any(), any()))
               .willReturn(Optional.empty());

       Member member = mock(Member.class);
       Category category = mock(Category.class);
       Trade trade = mock(Trade.class);

       given(memberRepository.getReferenceById(memberId)).willReturn(member);
       given(categoryRepository.getReferenceById(categoryId)).willReturn(category);

       //when
       investService.saveOrUpdateInvest(memberId, trade, categoryId, buyCount, buyPrice, "BUY");

       //then
       verify(investRepository, times(1)).save(any(Invest.class));


   }

   @Test
    @DisplayName("이미 보유한 종목 추가 매수시 평단가 계산에 반영되어야 한다.")
    void updateExistingInvestTest() {

       //given
       Invest existingInvest = Invest.builder()
               .investCount(BigDecimal.ONE)
               .investPrice(new BigDecimal("100"))
               .build();


       given(investRepository.findByMember_MemberIdAndCategory_CategoryId(any(), any()))
                .willReturn(Optional.of(existingInvest));


       BigDecimal newCount = BigDecimal.ONE;
       BigDecimal newPrice = new BigDecimal("200");


       //when
       investService.saveOrUpdateInvest(1L, mock(Trade.class), 10L, newCount, newPrice, "BUY");

       //then (평단가 공식)
       assertThat(existingInvest.getInvestCount()).isEqualByComparingTo("2");
       assertThat(existingInvest.getInvestPrice()).isEqualByComparingTo("150");
       verify(investRepository).save(existingInvest);




   }

   @Test
    @DisplayName("일부 매도 후 보유자산 갱신")
    void updateInvestAfterPartialSellTest() {


   }



}
