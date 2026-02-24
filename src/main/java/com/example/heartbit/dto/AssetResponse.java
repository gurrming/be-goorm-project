package com.example.heartbit.dto;

import com.example.heartbit.domain.Asset;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AssetResponse {
    private BigDecimal assetCash;
    private BigDecimal assetCanOrder;

    public static AssetResponse from(Asset asset) {
        return AssetResponse.builder()
                .assetCash(asset.getAssetCash())
                .assetCanOrder(asset.getAssetCanOrder())
                .build();
    }


}