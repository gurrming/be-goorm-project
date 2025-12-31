package com.example.heartbit.dto;

import com.example.heartbit.domain.Asset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AssetResponse {
    private BigDecimal assetCash;
    private BigDecimal totalAsset;

    public static AssetResponse from(Asset asset, BigDecimal totalEvaluateAmount) {
        return AssetResponse.builder()
                .assetCash(asset.getAssetCash())
                .totalAsset(asset.getAssetCash().add(totalEvaluateAmount))
                .build();
    }


}