package com.example.heartbit.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor // 생성자 자동생성
@AllArgsConstructor
@Builder
public class MemberRequestDto {
    private String memberEmail;
    private String memberPassword;
    private String memberNickname;
}
