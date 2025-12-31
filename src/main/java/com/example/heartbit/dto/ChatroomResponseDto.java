package com.example.heartbit.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatroomResponseDto {
    private Long chatroomId;
    private Long categoryId;
    private LocalDateTime chatroomTime;
    private String chatroomContent;

    private Long memberId;
    private String memberNickname; // 조인
}
