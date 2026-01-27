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


    public void blockCashForOrder(BigDecimal amount) {
        if (this.assetCanOrder.compareTo(amount) < 0) {
            throw new RuntimeException("주문 가능 금액이 부족합니다.");
        }
        this.assetCanOrder = this.assetCanOrder.subtract(amount);
    }

    // [주문 체결 시] 실제 보유 자산에서도 차감 (이미 주문 시 차감됐으므로 cash만 수정)
    public void confirmOrder(BigDecimal amount) {
        this.assetCash = this.assetCash.subtract(amount);
    }

    // [매수 체결 시] 실제 보유 자산(Cash) 차감 (CanOrder는 주문 시 이미 차감됨)
    public void confirmBuyOrder(BigDecimal executionAmount, BigDecimal blockedAmount) {
        // 1. 실제 자산 차감
        this.assetCash = this.assetCash.subtract(executionAmount);

        // 2. 차액(거스름돈) 계산: 1000 - 900 = 100원
        BigDecimal change = blockedAmount.subtract(executionAmount);

        // 3. 차액만큼 주문 가능 금액 복구
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            this.assetCanOrder = this.assetCanOrder.add(change);
        }
    }

    // [매도 체결 시] 정산금 입금 (Cash와 CanOrder 모두 증가)
    public void confirmSellOrder(BigDecimal amount) {
        this.assetCash = this.assetCash.add(amount);
        this.assetCanOrder = this.assetCanOrder.add(amount);
    }


    // [주문 취소 시] 주문 가능 금액 복구
    public void restoreCashFromCancel(BigDecimal amount) {
        this.assetCanOrder = this.assetCanOrder.add(amount);
    }
}
