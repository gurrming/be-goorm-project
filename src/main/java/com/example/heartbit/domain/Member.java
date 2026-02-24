package com.example.heartbit.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "member_id")
    private Long memberId;

    @Column(nullable = false, name = "member_email", length = 30, unique = true)
    private String memberEmail;

    @Column(nullable = false, name = "member_password", length = 100)
    private String memberPassword;

    @Column(nullable = false, name = "member_nickname", length = 20)
    private String memberNickname;

    @Builder
    public Member(String memberEmail, String memberPassword, String memberNickname) {
        this.memberEmail = memberEmail;
        this.memberPassword = memberPassword;
        this.memberNickname = memberNickname;
    }
}