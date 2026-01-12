package com.example.heartbit.service.auth;

import com.example.heartbit.dto.MemberRequestDto;
import com.example.heartbit.global.jwt.dto.IssuedTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public interface AuthService {
    public IssuedTokens login(MemberRequestDto.Login dto, HttpServletRequest request, HttpServletResponse response);
    String extractDeviceIdFromCookie(HttpServletRequest request);

    IssuedTokens issueNewToken(Long memberId, String deviceId);
    IssuedTokens reissue(HttpServletRequest request);
    ResponseCookie buildRefreshCookie(String refreshToken, long ttlSec);

    public void logout(HttpServletRequest request, HttpServletResponse response);
}
