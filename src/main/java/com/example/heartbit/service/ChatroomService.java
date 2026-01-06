package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Chatroom;
import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.ChatRequestDto;
import com.example.heartbit.dto.ChatResponseDto;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.ChatroomRepository;
import com.example.heartbit.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatroomService {
    private final MemberRepository memberRepository;
    private final ChatroomRepository chatroomRepository;
    private final CategoryRepository categoryRepository;

    // 채팅 목록 조회
    public List<ChatResponseDto> getChatroomsByCategory(Long categoryId) {

        List<Chatroom> chats = chatroomRepository.findByCategoryId(
                categoryId,
                PageRequest.of(0, 50)
        );

        return chats.stream()
                .map(ChatResponseDto::from)
                .toList();
    }

    // 채팅쓰기
    @Transactional
    public ChatResponseDto writeChat(ChatRequestDto requestDto) {

        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 memberId"));

        Category category = categoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 categoryId"));

        Chatroom chatroom =
                new Chatroom(category, member, requestDto.getChatContent());

        Chatroom saved = chatroomRepository.save(chatroom);

        return ChatResponseDto.from(saved);
    }



}
