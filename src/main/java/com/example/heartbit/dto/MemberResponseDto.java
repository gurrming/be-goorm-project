package com.example.heartbit.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Builder
public class MemberResponseDto {

    @Builder
    public record MemberTokenDTO(
            Long memberId,
            String accessToken,
            String refreshToken,
            String memberNickname
    ){}

    @Builder
    public record MemberInfo(
            String memberNickname
    ){}

    @Builder
    public record MemberReissueDTO(
            String accessToken,
            long accessExpiresInSec
    ){}
}
