package com.example.heartbit.service.member;

import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.MemberRequestDto;
import com.example.heartbit.dto.MemberResponseDto;
import org.springframework.stereotype.Service;

@Service
public interface MemberQueryService {
    Member getMemberByMemberId(Long memberId);
    boolean isMemberExist(Long memberId);

    MemberResponseDto.MemberInfo getMemberInfo(Long memberId);
    Member getCurrentMember();
    MemberResponseDto.EmailExistsDTO isExistsEmail(MemberRequestDto.Exists request);
}
