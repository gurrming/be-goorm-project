package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Chatroom;
import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.ChatRequestDto;
import com.example.heartbit.dto.ChatResponseDto;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.ChatroomRepository;
import com.example.heartbit.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatroomServiceTest {

    @InjectMocks
    private ChatroomService chatroomService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatroomRepository chatroomRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Nested
    @DisplayName("채팅 작성 테스트")
    class WriteChat {

        @Test
        @DisplayName("유효한 정보로 채팅을 작성하면 저장된 채팅 정보를 반환")
        void writeChat_Success() {
            // given
            Member member = Member.builder().memberId(1L).memberNickname("구르밍").build();
            Category category = Category.builder().categoryId(1L).categoryName("비트코인").build();
            ChatRequestDto requestDto = new ChatRequestDto(1L, "안녕하세요", 1L);
            Chatroom chatroom = new Chatroom(category, member, "안녕하세요");

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(chatroomRepository.save(any(Chatroom.class))).willReturn(chatroom);

            // when
            ChatResponseDto response = chatroomService.writeChat(requestDto);

            // then
            assertThat(response.getChatContent()).isEqualTo("안녕하세요");
            verify(chatroomRepository).save(any(Chatroom.class));
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID로 작성 시 예외발생")
        void writeChat_Fail_MemberNotFound() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThrows(IllegalArgumentException.class, () ->
                    chatroomService.writeChat(new ChatRequestDto(1L, "내용", 1L)));
        }
    }

    @Nested
    @DisplayName("채팅 조회 테스트")
    class GetChatrooms {

        @Test
        @DisplayName("lastChatroomId가 null이면 최신 채팅 목록을 조회")
        void getChatrooms_FirstEntrance() {
            // given
            Long categoryId = 1L;
            int size = 10;
            given(chatroomRepository.findLatestChatsByCategory(eq(categoryId), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            chatroomService.getChatroomsByCategory(categoryId, null, size);

            // then
            verify(chatroomRepository).findLatestChatsByCategory(eq(categoryId), any(Pageable.class));
        }

        @Test
        @DisplayName("lastChatroomId가 존재하면 이전 채팅 목록을 조회")
        void getChatrooms_WithCursor() {
            // given
            Long categoryId = 1L;
            Long lastChatroomId = 100L;
            int size = 10;
            given(chatroomRepository.findOlderChatsByCategory(eq(categoryId), eq(lastChatroomId), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            chatroomService.getChatroomsByCategory(categoryId, lastChatroomId, size);

            // then
            verify(chatroomRepository).findOlderChatsByCategory(eq(categoryId), eq(lastChatroomId), any(Pageable.class));
        }
    }
}