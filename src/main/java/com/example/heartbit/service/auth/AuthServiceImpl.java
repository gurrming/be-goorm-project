//package com.example.heartbit.service.auth;
//
//import com.example.heartbit.domain.Member;
//import com.example.heartbit.dto.MemberRequestDto;
//import com.example.heartbit.global.jwt.JwtTokenProvider;
//import com.example.heartbit.global.jwt.dto.IssuedTokens;
//import com.example.heartbit.repository.MemberRepository;
//import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.flywaydb.core.internal.parser.TokenType;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.Arrays;
//
//public class AuthServiceImpl implements AuthService{
//    private final MemberRepository memberRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final JwtTokenProvider jwtTokenProvider;
//
//    private static final String REFRESH_COOKIE = "refreshToken";
//    private static final String REFRESH_COOKIE_PATH = "/api/member";
//
//    @Override
//    public IssuedTokens login(MemberRequestDto.Login dto, HttpServletRequest request, HttpServletResponse response){
//        Member member = memberRepository.findByMemberEmail(dto.email())
//                .orElseThrow(()-> new IllegalArgumentException("이메일을 찾을 수 없습니다."));
//
//        if(!passwordEncoder.matches(dto.password(), member.getMemberPassword())){
//            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
//        }
//
//        String deviceId = extractDeviceIdFromCookie(request);
//        IssuedTokens issuedTokens = issueNewToken(member.getMemberId(), deviceId);
//        return issuedTokens;
//    }
//
//}
