package com.example.heartbit.service.member;

import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.MemberRequestDto;
import com.example.heartbit.global.jwt.JwtTokenProvider;
import com.example.heartbit.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandServiceImpl implements MemberCommandService{

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void signup(MemberRequestDto.Signup request){
        if(memberRepository.existsByMemberEmail(request.getEmail())){
            throw new IllegalArgumentException("이미 사용 중인 이메일 입니다.");
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member = Member.builder().memberEmail(request.getEmail())
                .memberPassword(encodedPassword).memberNickname(request.getNickname()).build();
        memberRepository.save(member);
    }

    public String login (MemberRequestDto.Login request){
        Member member = memberRepository.findByMemberEmail(request.memberEmail())
                .orElseThrow(()-> new IllegalArgumentException("Email 또는 비밀번호가 일치하지 않습니다."));
        if(!passwordEncoder.matches(request.getPassword(),member.getMemberPassword())){
            throw new IllegalArgumentException("Email 또는 비밀번호가 일치하지 않습니다.");
        }

        return jwtTokenProvider.createToken(String.valueOf(member.getMemberId()));
    }
}
