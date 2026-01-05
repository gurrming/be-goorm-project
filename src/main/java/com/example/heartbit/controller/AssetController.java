package com.example.heartbit.controller;

import com.example.heartbit.dto.AssetResponse;
import com.example.heartbit.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Tag(name = "자산 관리 API")
public class AssetController {

    private final AssetService assetService;

    @Operation(summary = "개인 자산 내역 조회", description = "멤버 ID를 통해 개인의 자산 내역을 조회합니다.")
    @GetMapping
    public ResponseEntity<AssetResponse> getMyAsset(@RequestParam Long memberId) {
        AssetResponse response = assetService.getAssetByMemberId(memberId);
        return ResponseEntity.ok(response);
    }

}
