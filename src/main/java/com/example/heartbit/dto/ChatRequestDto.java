package com.example.heartbit.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequestDto {
    private Long categoryId;
    private String chatContent;
    private Long memberId;
}