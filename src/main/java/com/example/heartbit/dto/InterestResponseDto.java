package com.example.heartbit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor // 역직렬화를 위해 필요
@AllArgsConstructor
@Getter
@Builder
public class InterestResponseDto {
    private Long interestId;
    private Long memberId;
    private Long categoryId;
}
