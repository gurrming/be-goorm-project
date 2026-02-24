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
import org.springframework.data.domain.Pageable;
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

    /**
         lastChatroomId -> cursor (없으면 null)
         size -> 가져올 개수
     */
    public List<ChatResponseDto> getChatroomsByCategory(
            Long categoryId,
            Long lastChatroomId,
            int size
    ) {
        Pageable pageable = PageRequest.of(0, size);

        List<Chatroom> chats;

        if (lastChatroomId == null) {
            // 채팅방 첫 입장 시
            chats = chatroomRepository.findLatestChatsByCategory(
                    categoryId,
                    pageable
            );
        } else {
            // cursor 있으면 이전 채팅 더 가져오기
            chats = chatroomRepository.findOlderChatsByCategory(
                    categoryId,
                    lastChatroomId,
                    pageable
            );
        }

        return chats.stream()
                .map(ChatResponseDto::from)
                .toList();
    }

    // 채팅 쓰기
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
