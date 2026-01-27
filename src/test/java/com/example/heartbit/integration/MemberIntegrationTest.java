package com.example.heartbit.integration;

import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.MemberRequestDto;
import com.example.heartbit.dto.MemberResponseDto;
import com.example.heartbit.global.jwt.JwtTokenProvider;
import com.example.heartbit.repository.MemberRepository;
import com.example.heartbit.service.AssetService;
import com.example.heartbit.service.member.MemberCommandServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // Mockito 환경 사용 (InvestIntegrationTest와 동일)
class MemberIntegrationTest {

    @InjectMocks
    private MemberCommandServiceImpl memberService; // 테스트할 실제 서비스 객체

    @Mock
    private MemberRepository memberRepository; // 가짜 DB

    @Mock
    private PasswordEncoder passwordEncoder; // 가짜 암호화 도구

    @Mock
    private JwtTokenProvider jwtTokenProvider; // 가짜 토큰 발급기

    @Mock
    private AssetService assetService;

    @Test
    @DisplayName("회원가입 시 비밀번호가 암호화되어 저장소에 저장된다.")
    void signupTest() {
        // given
        MemberRequestDto.Signup signupRequest = new MemberRequestDto.Signup(
                "test@naver.com",
                "1111",
                "구르밍"
        );

        // 암호화 동작 정의 (어떤 비밀번호가 들어오든 encodedPw)
        given(passwordEncoder.encode(any())).willReturn("encodedPw");

        // 이메일 중복 검사 통과 정의 (false = 중복 아님)
        given(memberRepository.existsByMemberEmail(any())).willReturn(false);

        // when
        memberService.signup(signupRequest);

        // then
        // 1. save 메서드가 호출되었는지 검증
        verify(memberRepository, times(1)).save(any(Member.class));
        // 2. 암호화 메서드가 호출되었는지 검증
        verify(passwordEncoder, times(1)).encode("1111");
    }

    @Test
    @DisplayName("로그인 성공 시 AccessToken과 RefreshToken이 정상 발급된다.")
    void loginSuccessTest() {
        // given
        String email = "test@naver.com";
        String rawPw = "1234";

        MemberRequestDto.Login loginRequest = new MemberRequestDto.Login(email, rawPw);

        // DB에 저장되어 있는 가짜 멤버 (비번은 암호화된 상태)
        Member mockMember = Member.builder()
                .memberId(1L)
                .memberEmail(email)
                .memberPassword("encodedPw")
                .memberNickname("로그인유저")
                .build();

        // 시나리오 설정
        given(memberRepository.findByMemberEmail(email)).willReturn(Optional.of(mockMember));
        given(passwordEncoder.matches(rawPw, "encodedPw")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(any())).willReturn("access-token-sample");
        given(jwtTokenProvider.createRefreshToken(any())).willReturn("refresh-token-sample");

        // when
        MemberResponseDto.MemberTokenDTO result = memberService.login(loginRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token-sample");
        assertThat(result.refreshToken()).isEqualTo("refresh-token-sample");

        // 검증: 토큰 발급기가 실제로 호출되었나?
        verify(jwtTokenProvider, times(1)).createAccessToken(any());
        verify(jwtTokenProvider, times(1)).createRefreshToken(any());
    }
}