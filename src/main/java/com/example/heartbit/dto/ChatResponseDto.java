package com.example.heartbit.dto;

import com.example.heartbit.domain.Chatroom;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponseDto {
    private Long chatId;
    private Long categoryId;
    private LocalDateTime chatTime;
    private String chatContent;

    private Long memberId;
    private String memberNickname;

    public static ChatResponseDto from(Chatroom chatroom) {
        return ChatResponseDto.builder()
                .chatId(chatroom.getChatroomId())
                .categoryId(chatroom.getCategory().getCategoryId())
                .chatTime(chatroom.getChatroomTime())
                .chatContent(chatroom.getChatroomContent())
                .memberId(chatroom.getMember().getMemberId())
                .memberNickname(chatroom.getMember().getMemberNickname())
                .build();
    }
}
