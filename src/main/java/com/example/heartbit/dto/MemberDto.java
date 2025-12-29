package com.example.heartbit.dto;

import lombok.Data;

@Data
public class MemberDto {
    private Long memberId;
    private String memberEmail;
    private String memberPassword;
    private String memberNickname;
}
