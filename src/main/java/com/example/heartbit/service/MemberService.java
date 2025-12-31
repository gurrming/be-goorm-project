package com.example.heartbit.service;

import com.example.heartbit.dto.MemberRequestDto;
import com.example.heartbit.dto.MemberResponseDto;
import com.example.heartbit.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

//    @Transactional
//    public MemberResponseDto signup(MemberRequestDto memberRequestDto){
//        return memberRepository.save(memberRequestDto);
//    }

    // 완전 임시
    @Transactional
    public MemberResponseDto signup(MemberRequestDto memberRequestDto) {

        return MemberResponseDto.builder()
                .memberId(1L) // 임시값
                .memberEmail(memberRequestDto.getMemberEmail())
                .memberNickname(memberRequestDto.getMemberNickname())
                .build();
    }

    // 완전 임시
    @Transactional
    public MemberResponseDto login(MemberRequestDto memberRequestDto) {

        return new MemberResponseDto(
                1L, // 임시 memberId
                memberRequestDto.getMemberEmail(),
                "임시유저"
        );
    }

    // 완전 임시
    @Transactional
    public MemberResponseDto logout(MemberRequestDto memberRequestDto) {

        return new MemberResponseDto(
                1L, // 임시 memberId
                memberRequestDto.getMemberEmail(),
                "로그아웃됨"
        );
    }

}
