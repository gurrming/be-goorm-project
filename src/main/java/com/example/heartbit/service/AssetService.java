package com.example.heartbit.service;

import com.example.heartbit.domain.Asset;
import com.example.heartbit.dto.AssetResponse;
import com.example.heartbit.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;

    public AssetResponse getAssetByMemberId(Long memberId) {
        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new RuntimeException("자산 정보를 찾을 수 없습니다."));

        return AssetResponse.builder()
                .assetCash(asset.getAssetCash())
                .totalAsset(asset.getTotalAsset())
                .build();
    }


}
