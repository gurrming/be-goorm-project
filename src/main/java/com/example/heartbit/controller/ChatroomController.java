package com.example.heartbit.controller;

import com.example.heartbit.dto.ChatroomRequestDto;
import com.example.heartbit.dto.ChatroomResponseDto;
import com.example.heartbit.service.ChatroomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatroom")
@RequiredArgsConstructor
@Tag(name = "채팅 API", description = "채팅 API")
public class ChatroomController {

    private final ChatroomService chatroomService;

    // 채팅 가져오기
    @Operation(summary = "채팅 내역 조회", description = "종목ID로 해당 채팅방의 채팅 로그들을 불러옵니다.")
    @GetMapping("/{categoryId}")
    public List<ChatroomResponseDto> getChatrooms(@PathVariable Long categoryId) {
        return chatroomService.getChatroomsByCategory(categoryId);
    }

    // 채팅쓰기
    @Operation(summary = "채팅 작성", description = "해당 채팅방에 작성한 채팅내용을 입력합니다.")
    @PostMapping
    public ChatroomResponseDto writeChatroom(@RequestBody ChatroomRequestDto requestDto) {
        return chatroomService.writeChat(requestDto);
    }


}
