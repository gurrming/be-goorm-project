package com.example.heartbit.controller;

import com.example.heartbit.dto.MemberRequestDto;
import com.example.heartbit.dto.MemberResponseDto;
import com.example.heartbit.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Tag(name = "유저관리 API", description = "사용자 관리 API")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임을 입력받아 신규 회원을 등록합니다.")
    @PostMapping("/signup")
    public MemberResponseDto signup(@RequestBody MemberRequestDto requestDto) {
        return memberService.signup(requestDto);
    }

    @Operation(summary = "로그인", description = "입력된 정보로 로그인 처리합니다.")
    @PostMapping("/login")
    public MemberResponseDto login(@RequestBody MemberRequestDto requestDto) {
        return memberService.login(requestDto);
    }

    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자의 세션/인증 정보를 종료합니다.")
    @PostMapping("/logout")
    public MemberResponseDto logout(@RequestBody MemberRequestDto requestDto) {
        return memberService.login(requestDto);
    }

}
