package com.example.heartbit.acceptance.chatroom;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.ChatRequestDto;
import com.example.heartbit.dto.ChatResponseDto;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.MemberRepository;
import com.example.heartbit.service.ChatroomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class ChatroomAcceptanceTest {

    @Autowired
    private ChatroomService chatroomService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Member testMember;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // 1. 멤버 생성
        testMember = memberRepository.findByMemberEmail("test@naver.com")
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .memberEmail("test@naver.com")
                        .memberPassword("1111")
                        .memberNickname("구르밍")
                        .build()));

        // 2. 카테고리 생성
        testCategory = categoryRepository.findBySymbol("BTC")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .categoryName("비트코인")
                        .symbol("BTC")
                        .build()));
    }

//    @Test
//    @DisplayName("사용자가 메시지를 보내면 DB에 정상적으로 저장된다.")
//    void writeChatTest() {
//        // given
//        ChatRequestDto request = ChatRequestDto.builder()
//                .categoryId(testCategory.getCategoryId())
//                .memberId(testMember.getMemberId())
//                .chatContent("한강 물온도 어때요?")
//                .build();
//
//        // when
//        ChatResponseDto response = chatroomService.writeChat(request);
//
//        // then
//        assertThat(response.getChatId()).isNotNull();
//        assertThat(response.getChatContent()).isEqualTo("한강 물온도 어때요?");
//    }
//
//    @Test
//    @DisplayName("특정 종목의 채팅 내역을 조회할 수 있다.")
//    void getChatroomsByCategoryTest() {
//        // given: 2개의 채팅 메시지 작성
//        chatroomService.writeChat(new ChatRequestDto(testCategory.getCategoryId(), "코인으로 돈벌기?", testMember.getMemberId()));
//        chatroomService.writeChat(new ChatRequestDto(testCategory.getCategoryId(), "가보자고", testMember.getMemberId()));
//
//        // when
//        List<ChatResponseDto> chats = chatroomService.getChatroomsByCategory(
//                testCategory.getCategoryId(),
//                null,
//                10
//        );
//
//        // then
//        assertThat(chats.size()).isGreaterThanOrEqualTo(2);
//        assertThat(chats.get(0).getChatContent()).isEqualTo("가보자고");
//    }
}