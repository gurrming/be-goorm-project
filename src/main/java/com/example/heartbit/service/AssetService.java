package com.example.heartbit.service;

import com.example.heartbit.domain.Asset;
import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.AssetResponse;
import com.example.heartbit.repository.AssetRepository;
import com.example.heartbit.repository.InvestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;
    private final InvestRepository investRepository;

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

    /**
     * 3. 주문 시 자산 차감 (OrderService에서 호출)
     */
    @Transactional
    public void deductCash(Long memberId, BigDecimal amount) {
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
        if (memberId == 1L) return;
        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("자산 정보를 찾을 수 없습니다."));

        asset.updateCash(asset.getAssetCash().add(amount));
    }
}


