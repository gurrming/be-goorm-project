package com.example.heartbit.repository;

import com.example.heartbit.domain.Chatroom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {

    // 종목 ID로 채팅로그 존재 여부
    boolean existsByCategory_CategoryId(Long categoryId);

    @EntityGraph(attributePaths = "member")
    @Query("""
        SELECT c
        FROM Chatroom c
        WHERE c.category.categoryId = :categoryId
        ORDER BY c.chatroomId DESC
    """)
    List<Chatroom> findLatestChatsByCategory(
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "member")
    @Query("""
        SELECT c
        FROM Chatroom c
        WHERE c.category.categoryId = :categoryId
          AND c.chatroomId < :lastChatroomId
        ORDER BY c.chatroomId DESC
    """)

    List<Chatroom> findOlderChatsByCategory(
            @Param("categoryId") Long categoryId,
            @Param("lastChatroomId") Long lastChatroomId,
            Pageable pageable
    );

}