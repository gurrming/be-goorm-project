package com.example.heartbit.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatroomDto {
    private Long chatroomId;
    private Long categoryId;
    private LocalDateTime chatroomTime;
    private String chatroomContent;
    private Long memberId;
}
