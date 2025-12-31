package com.example.heartbit.repository;

import com.example.heartbit.domain.Chatroom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {

    // 종목 ID로 채팅로그 있는지 확인
    boolean existsByCategoryId(Long categoryId);

    // 종목ID로 채팅로그 50 가져오기
    @EntityGraph(attributePaths = "member") // 없으면 멤버도 50번
    @Query("""
        SELECT c
        FROM Chatroom c
        WHERE c.category.categoryId = :categoryId
        ORDER BY c.chatroomTime DESC
    """)
    List<Chatroom> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);



}
