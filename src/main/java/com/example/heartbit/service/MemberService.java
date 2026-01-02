package com.example.heartbit.service;

import com.example.heartbit.dto.MemberRequestDto;
import com.example.heartbit.dto.MemberResponseDto;
import com.example.heartbit.repository.MemberRepository;
import com.example.heartbit.domain.Member;
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
    /**
     * 임시 로그인 사용자 반환
     * ❗ 실제 인증/인가 구현 전까지 사용
     */
    public Member getCurrentMember() {
        return Member.builder()
                .memberEmail("test@test.com")
                .memberNickname("TEST_USER")
                .memberPassword("TEMP") // null 싫으면 임시값
                .build();
    }

}
