package com.example.heartbit.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "asset")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long assetId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "asset_cash", precision = 18, scale = 8)
    private BigDecimal assetCash;

    @Builder
    public Asset(Member member, BigDecimal assetCash) {
        this.member = member;
        this.assetCash = (assetCash != null) ? assetCash : BigDecimal.ZERO; // 초기값 방어 코드
    }

    public void updateCash(BigDecimal amount) {
        this.assetCash = amount;
    }
}
