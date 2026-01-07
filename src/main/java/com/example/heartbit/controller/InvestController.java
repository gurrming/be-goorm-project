package com.example.heartbit.controller;

import com.example.heartbit.dto.invest.InvestPortfolioDto;
import com.example.heartbit.dto.invest.InvestQuantityDto;
import com.example.heartbit.service.InvestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 투자 관련 조회 API 컨트롤러
 * - 포트폴리오
 * - 투자 요약
 * - 종목별 수량 조회
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

    /**
     * 특정 종목 수량 조회
     * - categoryId, categoryName, symbol, quantity 반환
     */
    @GetMapping("/category/{categoryId}")
    @Operation(
            summary = "종목 수량 조회",
            description = "특정 종목(categoryId)의 투자 수량과 기본 정보를 조회합니다."
    )
    public ResponseEntity<InvestQuantityDto> getQuantity(@PathVariable("categoryId") Long categoryId) {
        InvestQuantityDto quantityDto = investService.getQuantityByCategoryId(categoryId);
        return ResponseEntity.ok(quantityDto);
    }

}