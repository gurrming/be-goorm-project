package com.example.heartbit.dto;

import com.example.heartbit.domain.Asset;
import com.example.heartbit.domain.Member;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class AssetCreateRequest {
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long memberId;

    private BigDecimal assetCash = new BigDecimal("500000000");

    public Asset toEntity(Member member) {
        return Asset.builder()
                .member(member)
                .assetCash(this.assetCash)
                .build();
    }
}
