package com.example.heartbit.controller;

import com.example.heartbit.dto.InvestResponse;
import com.example.heartbit.service.InvestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
@Tag(name = "투자", description = "투자 현황 조회 API")
public class InvestController {

    private final InvestService investService;

    @GetMapping("/summary")
    public ResponseEntity<InvestResponse> getInvestSummary(@RequestParam Long memberId,
                                                           @RequestParam(defaultValue = "0") int page, // 페이지 번호 (0부터 시작)
                                                           @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        InvestResponse response = investService.getInvestSummary(memberId, pageable);

        return ResponseEntity.ok(response);
    }



}