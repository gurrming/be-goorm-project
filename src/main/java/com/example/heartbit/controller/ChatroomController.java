package com.example.heartbit.controller;

import com.example.heartbit.dto.ChatResponseDto;
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

    @Operation(summary = "채팅 내역 조회", description = "종목별 채팅 내역을 종목 ID로 조회합니다.")
    @GetMapping("/{categoryId}")
    public List<ChatResponseDto> getChats(@PathVariable Long categoryId,
            @RequestParam(required = false) Long lastChatroomId,
            @RequestParam(defaultValue = "50") int size
    ) {
        return chatroomService.getChatroomsByCategory(
                categoryId,
                lastChatroomId,
                size
        );
    }


}
