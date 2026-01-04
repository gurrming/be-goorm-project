package com.example.heartbit.service.member;

import com.example.heartbit.dto.MemberRequestDto;

public interface MemberCommandService {
    void signup(MemberRequestDto.Signup signupRequest);
    String login(MemberRequestDto.Login loginRequest);
}
