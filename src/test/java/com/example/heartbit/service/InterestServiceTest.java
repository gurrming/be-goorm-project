package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Interest;
import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.InterestResponseDto;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.InterestRepository;
import com.example.heartbit.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

    @Mock
    private InterestRepository interestRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private InterestService interestService;

    @DisplayName("관심 종목 등록 시 회원과 카테고리를 찾아 저장한다.")
    @Test
    void saveInterest() {
        // given
        Long memberId = 1L;
        Long categoryId = 10L;
        Member member = Member.builder().memberId(memberId).build();
        Category category = Category.builder().categoryId(categoryId).build();

        Interest saveInterest = Interest.builder()
                .interestId(100L)
                .member(member)
                .category(category)
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(interestRepository.save(any(Interest.class))).willReturn(saveInterest);

        // when
        InterestResponseDto response = interestService.interestAdd(memberId, categoryId);

        // then
        assertThat(response.getInterestId()).isEqualTo(100L);
        assertThat(response.getMemberId()).isEqualTo(memberId);
        assertThat(response.getCategoryId()).isEqualTo(categoryId);
        verify(interestRepository).save(any(Interest.class));
    }

    @DisplayName("회원 ID로 관심 목록을 조회하면 리스트로 반환된다.")
    @Test
    void getInterest() {
        // given
        Long memberId = 1L;
        Member member = Member.builder()
                .memberId(memberId)
                .build();

        Category category = Category.builder()
                .categoryId(5L)
                .symbol("BTC")
                .build();

        Interest interest = Interest.builder()
                .interestId(1L)
                .member(member)
                .category(category)
                .build();

        given(interestRepository.findByMember_MemberId(memberId)).willReturn(List.of(interest));

        // when
        List<InterestResponseDto> result = interestService.getInterest(memberId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMemberId()).isEqualTo(memberId);
        assertThat(result.get(0).getCategoryId()).isEqualTo(5L);
    }

    @DisplayName("관심 ID를 통해 관심 등록을 삭제한다.")
    @Test
    void deleteInterest() {
        // given
        Long interestId = 500L;

        // when
        interestService.delete(interestId);

        // then
        verify(interestRepository, times(1)).deleteById(interestId);
    }
}