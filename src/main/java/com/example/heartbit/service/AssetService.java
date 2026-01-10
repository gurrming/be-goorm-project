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
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;
    private final InvestRepository investRepository;
    private final SimpMessagingTemplate messagingTemplate;


    @Transactional
    public void createInitialAsset(Member member) {
        Asset asset = Asset.builder()
                .member(member)
                .assetCash(new BigDecimal("500000000"))
                .build();
        assetRepository.save(asset);
    }

    // 자산 조회 (현금 + 현재 기준 평가금액 합계)
    public AssetResponse getAssetByMemberId(Long memberId) {
        // 1. 자산 조회
        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원의 자산 정보를 찾을 수 없습니다."));

        // 2. 평가 금액 계산
        BigDecimal totalEvaluateAmount = investRepository.findAllByMember_MemberId(memberId)
                .stream()
                .map(invest -> invest.getInvestCount().multiply(invest.getInvestPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. DTO 반환
        return AssetResponse.from(asset, totalEvaluateAmount);
    }

    // 5초마다 실시간 전송을 담당하던 스케줄러
    @Scheduled(fixedRate = 5000)
    public void sendAssetUpdate() {
        List<Asset> allAssets = assetRepository.findAll();

        for (Asset asset : allAssets) {
            Long memberId = asset.getMember().getMemberId();

            try {
                AssetResponse response = getAssetByMemberId(memberId);
                // 구독 경로: /topic/asset/{memberId}
                messagingTemplate.convertAndSend("/topic/asset/" + memberId, response);

            } catch (Exception e) {
                log.error("자산 업데이트 전송 실패 - 회원ID: {}", memberId, e);
            }
        }
    }

    /**
     * 주문 시 자산 차감
     */
    @Transactional
    public void deductCash(Long memberId, BigDecimal amount) {
        if (memberId.equals(5L)) return;
        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("자산 정보를 찾을 수 없습니다."));

        if (asset.getAssetCash().compareTo(amount) < 0) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }

        asset.updateCash(asset.getAssetCash().subtract(amount));
    }

    /**
     * 주문 취소 시 자산 환불
     */
    @Transactional
    public void refundCash(Long memberId, BigDecimal amount) {
        if (memberId.equals(5L)) return;
        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("자산 정보를 찾을 수 없습니다."));

        asset.updateCash(asset.getAssetCash().add(amount));
    }
}
