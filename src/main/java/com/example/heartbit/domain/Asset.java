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

    @Column(name = "asset_canorder", precision = 18, scale = 8)
    private BigDecimal assetCanOrder;

    @Builder
    public Asset(Member member, BigDecimal assetCash, BigDecimal assetCanOrder) {
        this.member = member;
        this.assetCash = (assetCash != null) ? assetCash : BigDecimal.ZERO; // 초기값 방어 코드
        this.assetCanOrder = (assetCanOrder != null) ? assetCanOrder : this.assetCash;
    }

    //보유 자산 증감 로직
    private void addCash(BigDecimal amount) {
        this.assetCash = this.assetCash.add(amount);
        if (this.assetCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("실제 잔액이 부족합니다.");
        }
    }

    //주문 가능 금액 증감 로직
    private void addCanOrder(BigDecimal amount) {
        this.assetCanOrder = this.assetCanOrder.add(amount);
        if (this.assetCanOrder.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("주문 가능 잔액이 부족합니다.");
        }
    }

    //매수 체결됐을때 보유 자산만 차감
    public void subtractOnlyCash(BigDecimal amount) {
        addCash(amount.negate());
    }

    //매수 주문시 주문 가능 금액 차감
    public void subtractOnlyCanOrder(BigDecimal amount) {
        addCanOrder(amount.negate());
    }

    //주문 취소됐을때 주문 가능금액 복구
    public void restoreCanOrder(BigDecimal amount) {
        addCanOrder(amount);
    }

    //매도 체결됐을때 보유 자산과 주문 가능금액 가산
    public void depositFull(BigDecimal amount) {
        addCash(amount);
        addCanOrder(amount);
    }


    public void confirmBuyOrder(BigDecimal executionAmount, BigDecimal blockedAmount) {
        //실제로 산 금액만큼 보유자산에서 차감
        subtractOnlyCash(executionAmount);

        //묶어놨던 돈에서 실제로 산 금액만큼 제외
        BigDecimal change = blockedAmount.subtract(executionAmount);

        //남은 거스름돈이 있다면 주문 가능 금액으로 돌려줌
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            restoreCanOrder(change);
        }
    }

}
