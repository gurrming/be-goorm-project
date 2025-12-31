package com.example.heartbit.controller;

import com.example.heartbit.dto.AssetResponse;
import com.example.heartbit.service.AssetService;
import com.example.heartbit.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    public ResponseEntity<AssetResponse> getMyAsset(@RequestParam Long memberId) {
        AssetResponse response = assetService.getAssetByMemberId(memberId);
        return ResponseEntity.ok(response);
    }

}
