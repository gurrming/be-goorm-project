package com.example.heartbit.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Bots {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "bot_id")
    private Long botId;


}