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
public class ChatroomResponseDto {
    private Long chatroomId;
    private Long categoryId;
    private LocalDateTime chatroomTime;
    private String chatroomContent;

    private Long memberId;
    private String memberNickname; // 조인

    public static ChatroomResponseDto from(Chatroom chatroom) {
        return ChatroomResponseDto.builder()
                .chatroomId(chatroom.getChatroomId())
                .categoryId(chatroom.getCategory().getCategoryId())
                .chatroomTime(chatroom.getChatroomTime())
                .chatroomContent(chatroom.getChatroomContent())
                .memberId(chatroom.getMember().getMemberId())
                .memberNickname(chatroom.getMember().getMemberNickname())
                .build();
    }

}
