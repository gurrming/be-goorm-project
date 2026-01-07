package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Interest;
import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.InterestResponseDto;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.InterestRepository;
import com.example.heartbit.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestService {

    private final InterestRepository interestRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;

    // 관심 등록
    @Transactional
    public InterestResponseDto interestAdd(Long memberId, Long categoryId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Category category = categoryRepository.findById(categoryId).orElseThrow();

        Interest saved = interestRepository.save(Interest.create(member, category));

        return InterestResponseDto.builder()
                .interestId(saved.getInterestId())
                .memberId(memberId)
                .categoryId(categoryId)
                .build();
    }


    // 관심해놓은 목록 불러오기
    public List<InterestResponseDto> getInterest(Long memberId) {
        return interestRepository.findByMember_MemberId(memberId).stream()
                .map(i -> InterestResponseDto.builder()
                        .interestId(i.getInterestId())
                        .memberId(i.getMember().getMemberId())
                        .categoryId(i.getCategory().getCategoryId())
                        .build())
                .toList();
    }

    // 관심 해제
    @Transactional
    public void delete(Long interestId) {
        interestRepository.deleteById(interestId);
    }
}
