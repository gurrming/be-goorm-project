package com.example.heartbit.domain;

import jakarta.persistence.*;
import lombok.*;

//테이블 컬럼들이 구성되어야하는 클래스
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "sentiment_result")// db 테이블 명(긍/부정)
@AllArgsConstructor // Builder 사용을 위해 추가
@Builder            // 데이터 생성을 편리하게 하기 위해 추가
@ToString
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

    @Column(name = "news_score")
    private Double newsScore;

    @Column(name = "community_score")
    private Double communityScore;

}
