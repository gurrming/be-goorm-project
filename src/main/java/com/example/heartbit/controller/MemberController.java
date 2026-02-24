package com.example.heartbit.controller;

import com.example.heartbit.dto.MemberRequestDto;
import com.example.heartbit.dto.MemberResponseDto;
import com.example.heartbit.global.jwt.dto.IssuedTokens;
import com.example.heartbit.global.response.ApiResponse;
import com.example.heartbit.service.member.MemberCommandService;
import com.example.heartbit.service.member.MemberQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Validated
@Tag(name = "유저관리 API", description = "사용자 관리 API")
public class MemberController {

    private final MemberCommandService memberCommandService;
    private  final MemberQueryService memberQueryService;

    @Operation(summary = "이메일 중복확인")
    @PostMapping("/exists")
    public ResponseEntity<MemberResponseDto.EmailExistsDTO> exists(@Valid @RequestBody MemberRequestDto.Exists requestDto){
        MemberResponseDto.EmailExistsDTO response = memberQueryService.isExistsEmail(requestDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임을 입력받아 신규 회원을 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody MemberRequestDto.Signup requestDto) {
        memberCommandService.signup(requestDto);
        return ResponseEntity.ok("회원가입 성공");
    }

    @Operation(summary = "로그인", description = "입력된 정보로 로그인 처리합니다.(토큰을 쿠키에 저장)")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<MemberResponseDto.MemberTokenDTO>> login(@Valid @RequestBody MemberRequestDto.Login requestDto, HttpServletResponse response) {
        MemberResponseDto.MemberTokenDTO tokenDTO = memberCommandService.login(requestDto);

//        Cookie accessCookie = new Cookie("accessToken", tokenDTO.accessToken());
//        accessCookie.setHttpOnly(true);
//        accessCookie.setPath("/");
//        accessCookie.setMaxAge(30*60);
//
//        Cookie refreshCookie = new Cookie("refreshToken", tokenDTO.refreshToken());
//        refreshCookie.setHttpOnly(true);
//        refreshCookie.setPath("/");
//        refreshCookie.setMaxAge(7*24*60*60);
//
//        response.addCookie(accessCookie);
//        response.addCookie(refreshCookie);
//
//        return ResponseEntity.ok(ApiResponse.onSuccess(tokenDTO));
        // 1. Access Token 쿠키 생성 (SameSite=None, Secure 적용)
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", tokenDTO.accessToken())
                .path("/")
                .sameSite("None")  // 중요: 서로 다른 도메인 간 허용
                .secure(true)      // 중요: HTTPS 환경 필수
                .httpOnly(true)    // JS 접근 불가 (보안)
                .maxAge(30 * 60)   // 30분
                .build();

        // 2. Refresh Token 쿠키 생성
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokenDTO.refreshToken())
                .path("/")
                .sameSite("None")  // 중요
                .secure(true)      // 중요
                .httpOnly(true)
                .maxAge(7 * 24 * 60 * 60) // 7일
                .build();

        // 3. 응답 헤더에 추가 (addHeader 사용)
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 프론트엔드에서 Authorization 헤더로도 토큰을 읽을 수 있도록 지원하려면 아래 줄 추가
        // response.setHeader("Authorization", "Bearer " + tokenDTO.accessToken());

        return ResponseEntity.ok(ApiResponse.onSuccess(tokenDTO));
    }

    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자의 세션/인증 정보를 종료합니다.(쿠키 삭제)")
    @PostMapping("/logout")
    public ResponseEntity<String> logout( HttpServletResponse response) {
//        Cookie accessCookie = new Cookie("accessToken", null);
//        accessCookie.setMaxAge(0);
//        accessCookie.setPath("/");
//
//        Cookie refreshCookie = new Cookie("refreshToken", null);
//        refreshCookie.setMaxAge(0);
//        refreshCookie.setPath("/");
//
//        response.addCookie(accessCookie);
//        response.addCookie(refreshCookie);
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "") // 빈 값
                .path("/")
                .sameSite("None")
                .secure(true)
                .httpOnly(true)
                .maxAge(0) // 즉시 만료
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .path("/")
                .sameSite("None")
                .secure(true)
                .httpOnly(true)
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok("로그아웃 성공");
    }

    @Operation(summary = "토큰 재발급", description = "refresh 토큰을 사용하여 토큰 재발급")
    @PostMapping("/reissue")
    public ResponseEntity<MemberResponseDto.MemberReissueDTO> reissue(HttpServletRequest request, HttpServletResponse response){
        IssuedTokens issued = memberCommandService.reissue(request);

//        ResponseCookie refreshCookie = memberCommandService.buildRefreshCookie(
//                issued.refreshToken(), issued.refreshExpiresInSec()
//        );
//        response.addHeader("Set-Cookie", refreshCookie.toString());
//
//        MemberResponseDto.MemberReissueDTO memberReissueDTO = MemberResponseDto.MemberReissueDTO
//                .builder()
//                .accessToken(issued.accessToken())
//                .accessExpiresInSec(issued.accessExpiresInSec())
//                .build();
        // 1. Refresh Token 쿠키 생성 (배포 환경 대응)
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", issued.refreshToken())
                .path("/")
                .sameSite("None")  // 필수
                .secure(true)      // 필수 (HTTPS)
                .httpOnly(true)
                .maxAge(issued.refreshExpiresInSec()) // 7일
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 2. Access Token 쿠키 생성 (선택 사항: 프론트에서 쿠키로 쓴다면 필수)
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", issued.accessToken())
                .path("/")
                .sameSite("None")
                .secure(true)
                .httpOnly(true)
                .maxAge(issued.accessExpiresInSec()) // 30분
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        // 3. 응답 DTO 반환
        MemberResponseDto.MemberReissueDTO memberReissueDTO = MemberResponseDto.MemberReissueDTO
                .builder()
                .accessToken(issued.accessToken())
                .accessExpiresInSec(issued.accessExpiresInSec())
                .build();

        return ResponseEntity.ok(memberReissueDTO);
    }

}
