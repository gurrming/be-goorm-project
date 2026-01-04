package com.example.heartbit.service.member;

import com.example.heartbit.dto.MemberRequestDto;
import com.example.heartbit.dto.MemberResponseDto;

public interface MemberCommandService {
    void signup(MemberRequestDto.Signup signupRequest);
    MemberResponseDto.MemberTokenDTO login(MemberRequestDto.Login loginRequest);
}
