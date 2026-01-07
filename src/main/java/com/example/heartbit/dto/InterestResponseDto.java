package com.example.heartbit.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InterestResponseDto {
    private Long interestId;
    private Long memberId;
    private Long categoryId;
}
