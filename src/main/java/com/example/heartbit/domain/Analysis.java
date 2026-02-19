package com.example.heartbit.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "sentiment_result")
@AllArgsConstructor
@Builder
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "category_id", nullable = false, unique = true)
    private Long categoryId;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "total_label", length = 10)
    private String totalLabel;

    // [중요] score 필드 삭제하고 result를 Double로 선언
    // DB의 'real' 타입은 자바의 Double로 매핑 가능합니다.
    @Column(name = "news_result")
    private Double newsResult;

    @Column(name = "community_result")
    private Double communityResult;

    @Column(name = "summary")
    private String summary;

    @Column(name = "full_report", columnDefinition = "TEXT")
    private String fullReport;

    @Column(name = "rsi")
    private Double rsi;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}