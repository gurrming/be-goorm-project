package com.example.heartbit.controller;

import com.example.heartbit.dto.invest.InvestPortfolioDto;
import com.example.heartbit.service.InvestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 투자 관련 조회 API 컨트롤러
 * - 포트폴리오
 * - 투자 요약
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invest")
@Tag(name = "투자", description = "투자 포트폴리오 및 투자 현황 조회 API")
public class InvestController {

    private final InvestService investService;
    /**
     * 투자 포트폴리오 조회
     * - 종목별 투자 현황
     * - 수익 / 평가금액 포함
     */
    @GetMapping("/portfolio")
    @Operation(
            summary = "포트폴리오 조회",
            description = "사용자의 전체 투자 포트폴리오와 종목별 자산 내역을 조회합니다."
    )
    public InvestPortfolioDto getPortfolio() {
        return investService.getPortfolio();
    }
}
