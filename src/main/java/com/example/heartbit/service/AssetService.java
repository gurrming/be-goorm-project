package com.example.heartbit.service;

import com.example.heartbit.domain.Asset;
import com.example.heartbit.dto.AssetResponse;
import com.example.heartbit.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;

    //이 부분은 임시용. 나중에 회원가입과 동시에 자산 생성이 되면서 5억을 넣어주는 로직구현해야함.
    public AssetResponse getAssetByMemberId(Long memberId) {

        return AssetResponse.builder()
                .assetCash(new BigDecimal("100000"))
                .totalAsset(new BigDecimal("1500000"))
                .build();
    }


}
