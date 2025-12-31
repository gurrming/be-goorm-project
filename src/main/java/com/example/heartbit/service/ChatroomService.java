package com.example.heartbit.service;

import com.example.heartbit.repository.ChatroomRepository;
import com.example.heartbit.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatroomService {
    private final MemberRepository memberRepository;
    private final ChatroomRepository chatroomRepository;



}
