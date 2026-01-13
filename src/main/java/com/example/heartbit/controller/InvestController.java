package com.example.heartbit.controller;

import com.example.heartbit.dto.InvestResponse;
import com.example.heartbit.service.InvestService;
import com.example.heartbit.service.TradeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 투자 관련 조회 API 컨트롤러
 * - 포트폴리오
 * - 투자 요약
 * - 종목별 수량 조회
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invest")
@Tag(name = "투자", description = "투자 현황 조회 API")
public class InvestController {

    private final InvestService investService;
    private final TradeService tradeService;

    @GetMapping("/summary")
    public ResponseEntity<InvestResponse> getInvestSummary(@RequestParam Long memberId) {
        // 1. TradeService에서 메모리에 관리 중인 전체 시세 맵을 가져옴
        Map<Long, BigDecimal> currentPriceMap = tradeService.getAllCurrentPrices();

        // 2. 서비스 호출 시 시세 맵을 인자로 전달
        InvestResponse response = investService.getInvestSummary(memberId, currentPriceMap);

        return ResponseEntity.ok(response);
    }



}