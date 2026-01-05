package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.invest.InvestAssetDto;
import com.example.heartbit.dto.invest.InvestPortfolioDto;
import com.example.heartbit.dto.invest.InvestSummaryDto;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.InvestRepository;
import com.example.heartbit.service.member.MemberQueryServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class InvestService {

    // 투자 내역 조회용
    private final InvestRepository investRepository;

    // 종목 정보
    private final CategoryRepository categoryRepository;

    // 로그인 사용자 조회
    private final MemberQueryServiceImpl memberService;

    public InvestPortfolioDto getPortfolio() {

        // 1 현재 로그인한 사용자 조회

        Member member = memberService.getCurrentMember();

        // 2 전체 투자 요약 계산용 변수
        BigDecimal totalBuyAmount = BigDecimal.ZERO;       // 총 매수 금액
        BigDecimal totalEvaluateAmount = BigDecimal.ZERO;  // 총 평가 금액

        // 3 종목별 투자 현황 리스트
        List<InvestAssetDto> assets = new ArrayList<>();

        // 4 전체 종목 조회
        List<Category> categories = categoryRepository.findAll();

        // 5 종목별 투자 현황 계산
        for (Category category : categories) {

            // 5-1 회원 + 종목 기준 보유 수량 조회
            BigDecimal quantity =
                    investRepository.findTotalHoldingByMemberAndCategory(member, category);

            // 보유하지 않은 종목은 포트폴리오에서 제외
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // 5-2 회원 + 종목 기준 평균 매수가
            BigDecimal avgBuyPrice =
                    investRepository.findAvgBuyPriceByMemberAndCategory(member, category);

            // 5-3 매수 금액 = 평균 매수가 × 수량
            BigDecimal buyAmount =
                    avgBuyPrice.multiply(quantity);

            // 5-4 현재가
            // 현재는 Category에 가격이 있다고 가정
            BigDecimal currentPrice = category.getPrice();

            // 5-5 평가 금액 = 현재가 × 수량
            BigDecimal evaluateAmount =
                    currentPrice.multiply(quantity);

            // 5-6 평가 손익 = 평가 금액 - 매수 금액
            BigDecimal profit =
                    evaluateAmount.subtract(buyAmount);

            // 5-7 전체 합계 누적
            totalBuyAmount = totalBuyAmount.add(buyAmount);
            totalEvaluateAmount = totalEvaluateAmount.add(evaluateAmount);

            // 5-8 종목별 투자 현황 DTO 생성
            assets.add(new InvestAssetDto(
                    category.getCategoryId(),
                    category.getCategoryName(),
                    quantity,
                    avgBuyPrice,
                    buyAmount,
                    evaluateAmount,
                    profit
            ));
        }

        // 6 총 평가 손익 = 총 평가 금액 - 총 매수 금액
        BigDecimal totalProfit =
                totalEvaluateAmount.subtract(totalBuyAmount);

        // 7 총 평가 수익률 계산
        // → 매수 금액이 0인 경우 0으로 처리 (0으로 나누기 방지)
        BigDecimal totalProfitRate = BigDecimal.ZERO;

        if (totalBuyAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitRate = totalProfit
                    .divide(totalBuyAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // 8 투자 요약 DTO 생성
        InvestSummaryDto summary = new InvestSummaryDto(
                totalBuyAmount,
                totalEvaluateAmount,
                totalProfit,
                totalProfitRate
        );

        // 9 포트폴리오 DTO 반환
        return new InvestPortfolioDto(summary, assets);
    }
}
