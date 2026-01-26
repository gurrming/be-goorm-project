package com.example.heartbit.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatroom")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Chatroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatroom_id")
    private Long chatroomId;

    // FK: category_id -> Category 엔티티
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // FK: member_id -> Member 엔티티
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "chatroom_time")
    private LocalDateTime chatroomTime;

    @Column(name = "chatroom_content", length = 500)
    private String chatroomContent;

    public Chatroom(Category category, Member member, String chatroomContent) {
        this.category = category;
        this.member = member;
        this.chatroomContent = chatroomContent;
        this.chatroomTime = LocalDateTime.now();
    }


}
