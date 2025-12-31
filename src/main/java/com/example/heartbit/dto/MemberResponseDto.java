package com.example.heartbit.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponseDto {
    private Long memberId;
    private String memberEmail;
    private String memberNickname;
    // 비밀번호 안넣기
}
