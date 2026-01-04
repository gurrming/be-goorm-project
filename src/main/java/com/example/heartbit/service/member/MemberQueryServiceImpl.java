package com.example.heartbit.service.member;

import com.example.heartbit.dto.MemberRequestDto;
import com.example.heartbit.dto.MemberResponseDto;
import com.example.heartbit.repository.MemberRepository;
import com.example.heartbit.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryService{

    private final MemberRepository memberRepository;

    @Override
    public boolean isMemberExist(Long memberId){
        return memberRepository.existsById(memberId);
    }

    @Override
    public Member getMemberByMemberId(Long memberId){
        return memberRepository.findById(memberId)
                .orElseThrow(()-> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + memberId));
    }

    @Override
    public MemberResponseDto.MemberInfo getMemberInfo(Long memberId){
        Member member = getMemberByMemberId(memberId);
        return MemberResponseDto.MemberInfo.builder()
                .memberNickname(member.getMemberNickname())
                .build();
    }

}
