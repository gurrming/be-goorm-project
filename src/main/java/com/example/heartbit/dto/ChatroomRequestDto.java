package com.example.heartbit.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatroomRequestDto {
    private Long categoryId;
    private String chatroomContent;
    private Long memberId;
}
