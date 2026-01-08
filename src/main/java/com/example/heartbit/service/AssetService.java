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
import java.util.Optional;

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

    //이 부분은 임시용. 나중에 회원가입과 동시에 자산 생성이 되면서 5억을 넣어주는 로직구현해야함.
    public AssetResponse getAssetByMemberId(Long memberId) {
        // 1. 자산 조회
        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원의 자산 정보를 찾을 수 없습니다."));

        // 2. 평가 금액 계산 (수정된 부분)
        BigDecimal totalEvaluateAmount = investRepository.findByMember_MemberId(memberId)
                .stream()
                .map(invest -> invest.getInvestCount().multiply(invest.getInvestPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. DTO 반환
        return AssetResponse.from(asset, totalEvaluateAmount);
    }

    @Scheduled(fixedRate = 5000) // 1초(1000ms)마다 자동 실행
    public void sendAssetUpdate() {
        // 1. 현재 접속 중인 사용자의 ID 리스트를 가져와야 합니다.
        // (지금은 테스트를 위해 특정 ID를 지정하거나, 전체 회원을 순회할 수 있습니다.)
        // 여기서는 모든 자산 정보를 순회하며 웹소켓을 쏘는 예시입니다.

        List<Asset> allAssets = assetRepository.findAll();

        for (Asset asset : allAssets) {
            Long memberId = asset.getMember().getMemberId();

            try {
                // 기존에 잘 만들어둔 계산 로직 호출
                AssetResponse response = getAssetByMemberId(memberId);

                // 특정 사용자의 개인 채널로 전송
                // 구독 경로: /sub/asset/{memberId}
                messagingTemplate.convertAndSend("/topic/asset/" + memberId, response);

            } catch (Exception e) {
                // 특정 유저 계산 실패 시 로그만 남기고 다음 유저로 진행
                log.error("자산 업데이트 전송 실패 - 회원ID: {}", memberId, e);
            }
        }
    }

    /**
     * 3. 주문 시 자산 차감 (OrderService에서 호출)
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
     * 4. 주문 취소 시 자산 환불 (OrderService에서 호출)
     */
    @Transactional
    public void refundCash(Long memberId, BigDecimal amount) {
        if (memberId.equals(5L)) return;
        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("자산 정보를 찾을 수 없습니다."));

        asset.updateCash(asset.getAssetCash().add(amount));
    }
}


