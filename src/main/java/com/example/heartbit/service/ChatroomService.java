package com.example.heartbit.service;

import com.example.heartbit.dto.ChatroomRequestDto;
import com.example.heartbit.dto.ChatroomResponseDto;
import com.example.heartbit.repository.ChatroomRepository;
import com.example.heartbit.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatroomService {
    private final MemberRepository memberRepository;
    private final ChatroomRepository chatroomRepository;

    // 채팅 목록 조회 (완전 임시)
    public List<ChatroomResponseDto> getChatroomsByCategory(Long categoryId) {

        return List.of(
                new ChatroomResponseDto(
                        1L,
                        categoryId,
                        LocalDateTime.now(),
                        "임시 채팅 내용입니다.",
                        1L,
                        "임시유저"
                )
        );
    }

    // 채팅쓰기 (완전 임시)
    @Transactional
    public ChatroomResponseDto writeChat(ChatroomRequestDto requestDto) {

        return new ChatroomResponseDto(
                1L,
                requestDto.getCategoryId(),
                LocalDateTime.now(),
                requestDto.getChatroomContent(),
                requestDto.getMemberId(),
                "임시유저"
        );
    }


}
