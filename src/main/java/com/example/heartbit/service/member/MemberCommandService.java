package com.example.heartbit.service.member;

import com.example.heartbit.dto.MemberRequestDto;
import com.example.heartbit.dto.MemberResponseDto;
import com.example.heartbit.global.jwt.dto.IssuedTokens;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

public interface MemberCommandService {
    void signup(MemberRequestDto.Signup signupRequest);
    MemberResponseDto.MemberTokenDTO login(MemberRequestDto.Login loginRequest);
    IssuedTokens issueNewTokens(Long memberId, String deviceId);
    IssuedTokens reissue(HttpServletRequest request);
    ResponseCookie buildRefreshCookie(String refreshToken, long ttlSec);
    String extractRefreshFromCookie(HttpServletRequest req);
}
