package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Chatroom;
import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.ChatRequestDto;
import com.example.heartbit.dto.ChatResponseDto;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.ChatroomRepository;
import com.example.heartbit.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatroomServiceTest {

    @InjectMocks ChatroomService chatroomService;
    @Mock ChatroomRepository chatroomRepository;
    @Mock MemberRepository memberRepository;
    @Mock CategoryRepository categoryRepository;

    @Test
    void chatSaveSuccessTest() {
        // given
        ChatRequestDto request = new ChatRequestDto(1L, "내용", 1L);

        Member member = Member.builder().memberId(1L).memberNickname("닉네임").build();
        Category category = Category.builder().categoryId(1L).categoryName("비트코인").build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        Chatroom savedChatroom = Chatroom.builder()
                .member(member)
                .category(category)
                .chatroomContent("내용")
                .build();

        given(chatroomRepository.save(any())).willReturn(savedChatroom);

        // when
        ChatResponseDto response = chatroomService.writeChat(request);

        // then
        assertThat(response.getChatContent()).isEqualTo("내용");
        verify(chatroomRepository).save(any());
    }
}