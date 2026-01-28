package com.example.heartbit.service;

import com.example.heartbit.domain.Asset;
import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.AssetResponse;
import com.example.heartbit.repository.AssetRepository;
import com.example.heartbit.repository.InvestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용
public class AssetService {

    private final AssetRepository assetRepository;
    private final InvestRepository investRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional // 쓰기 작업이므로 붙여야 함
    public void createInitialAsset(Member member) {
        BigDecimal initialAmount = new BigDecimal("500000000");
        Asset asset = Asset.builder()
                .member(member)
                .assetCash(initialAmount)
                .assetCanOrder(initialAmount) // 초기화 시 주문 가능 금액도 현금과 동일하게 설정
                .build();
        assetRepository.save(asset);
    }

    // 자산 조회 (현금 + 현재 기준 평가금액 합계)
    public AssetResponse getAssetByMemberId(Long memberId) {
        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원의 자산 정보를 찾을 수 없습니다."));

        BigDecimal totalAsset = investRepository.findAllByMember_MemberId(memberId)
                .stream()
                .map(invest -> invest.getInvestCount().multiply(invest.getInvestPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AssetResponse.from(asset, totalAsset);
    }

    // 5초마다 실시간 전송을 담당하던 스케줄러
    @Scheduled(fixedRate = 5000)
    public void sendAssetUpdate() {
        List<Asset> allAssets = assetRepository.findAll();

        for (Asset asset : allAssets) {
            Long memberId = asset.getMember().getMemberId();
            try {
                AssetResponse response = getAssetByMemberId(memberId);
                messagingTemplate.convertAndSend("/topic/asset/" + memberId, response);
            } catch (Exception e) {
                log.error("자산 업데이트 전송 실패 - 회원ID: {}", memberId, e);
            }
        }
    }



    // 1. 주문 시 자산 차감 (매수 주문 - 주문 가능 금액만 차감)
    @Transactional
    public void blockCash(Long memberId, BigDecimal amount) {
        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("자산을 찾을 수 없습니다."));
        // Asset 엔티티의 비즈니스 로직 호출
        asset.blockCashForOrder(amount);
    }

    // 2. 주문 취소 시 자산 복구 (매수 취소 - 주문 가능 금액 복구)
    @Transactional
    public void restoreCash(Long memberId, BigDecimal amount) {
        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("자산을 찾을 수 없습니다."));
        asset.restoreCashFromCancel(amount);
    }

    // 3. 매수 체결 정산 (보유 자산 실차감 - 현금 감소)
    @Transactional
    public void settleBuyTrade(Long memberId, BigDecimal executionAmount, BigDecimal blockedAmount) {
        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("자산을 찾을 수 없습니다."));

        // 수정된 메서드 호출
        asset.confirmBuyOrder(executionAmount, blockedAmount);
    }

    // 4. 매도 체결 정산 (판매 대금 입금 - 현금 & 주문가능금액 증가)
    @Transactional
    public void settleSellTrade(Long memberId, BigDecimal amount) {
        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("자산을 찾을 수 없습니다."));
        asset.confirmSellOrder(amount);
    }
}