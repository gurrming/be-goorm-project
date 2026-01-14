package com.example.heartbit.service.member;

import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.MemberRequestDto;
import com.example.heartbit.dto.MemberResponseDto;
import com.example.heartbit.global.jwt.JwtTokenProvider;
import com.example.heartbit.global.jwt.dto.IssuedTokens;
import com.example.heartbit.repository.MemberRepository;
import com.example.heartbit.service.AssetService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandServiceImpl implements MemberCommandService{

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AssetService assetService;

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/";

    @Override
    public void signup(MemberRequestDto.Signup request){
        if(memberRepository.existsByMemberEmail(request.email())){
            throw new IllegalArgumentException("이미 사용 중인 이메일 입니다.");
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        Member member = Member.builder().memberEmail(request.email())
                .memberPassword(encodedPassword).memberNickname(request.nickname()).build();
        memberRepository.save(member);

        assetService.createInitialAsset(member);
    }

    public MemberResponseDto.MemberTokenDTO login (MemberRequestDto.Login request){
        Member member = memberRepository.findByMemberEmail(request.email())
                .orElseThrow(()-> new IllegalArgumentException("Email 또는 비밀번호가 일치하지 않습니다."));
        if(!passwordEncoder.matches(request.password(),member.getMemberPassword())){
            throw new IllegalArgumentException("Email 또는 비밀번호가 일치하지 않습니다.");
        }

        String memberId = String.valueOf(member.getMemberId());
        String accessToken = jwtTokenProvider.createAccessToken(memberId);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId);
        String memberNickname = member.getMemberNickname();

        return new MemberResponseDto.MemberTokenDTO(member.getMemberId(),accessToken,refreshToken, memberNickname);
    }

    public IssuedTokens issueNewTokens(Long memberId, String deviceId){
        return jwtTokenProvider.issueNewTokens(memberId, deviceId);
    }

    public IssuedTokens reissue (HttpServletRequest request){
        String refreshToken = extractRefreshFromCookie(request);

        if(refreshToken == null){
            throw new IllegalArgumentException("Refresh Token이 존재하지 않습니다.");
        }

        Claims claims = jwtTokenProvider.getClaims(refreshToken);
        Long memberId = Long.parseLong(claims.getSubject());
        String deviceId = claims.get("did", String.class);

        return issueNewTokens(memberId, deviceId);
    }

    // ✅ 쿠키 생성 유틸 (AuthServiceImpl 로직 반영)
    public ResponseCookie buildRefreshCookie(String refreshToken, long ttlSec) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true) // HTTPS 환경 필수 (로컬 개발시 false 고려)
                .sameSite("None")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofSeconds(ttlSec))
                .build();
    }

    // 🍪 쿠키 추출 헬퍼 메서드
    public String extractRefreshFromCookie(HttpServletRequest req) {
        return Arrays.stream(Optional.ofNullable(req.getCookies()).orElse(new Cookie[0]))
                .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }


}
