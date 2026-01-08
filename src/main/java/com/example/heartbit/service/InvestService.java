package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Member;
import com.example.heartbit.dto.invest.InvestAssetDto;
import com.example.heartbit.dto.invest.InvestPortfolioDto;
import com.example.heartbit.dto.invest.InvestSummaryDto;
import com.example.heartbit.dto.invest.InvestQuantityDto;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.InvestRepository;
import com.example.heartbit.service.member.MemberQueryServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InvestService {

    private final InvestRepository investRepository;
    private final CategoryRepository categoryRepository;
    private final MemberQueryServiceImpl memberService;
    private final TradeService tradeService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 전체 포트폴리오 조회
     */
    public InvestPortfolioDto getPortfolio() {

        Member member = memberService.getCurrentMember();
        List<InvestAssetDto> assets = new ArrayList<>();
        List<Category> categories = categoryRepository.findAll();

        BigDecimal totalBuyAmount = BigDecimal.ZERO;
        BigDecimal totalEvaluateAmount = BigDecimal.ZERO;

        boolean hasHolding = false; // 보유 자산 존재 여부

        // 체결된 종목만 asset 생성 및 합계 계산
        for (Category category : categories) {

            BigDecimal quantity = investRepository.findTotalHoldingByMemberAndCategory(member, category);
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // 체결되지 않은 종목 → 건너뜀
            }

            hasHolding = true; // 하나라도 보유 자산 있음

            BigDecimal avgBuyPrice = investRepository.findAvgBuyPriceByMemberAndCategory(member, category);
            if (avgBuyPrice == null) avgBuyPrice = BigDecimal.ZERO;

            BigDecimal buyAmount = avgBuyPrice.multiply(quantity);

            // 체결된 종목 기준 최근 체결가
            BigDecimal currentPrice = Optional.ofNullable(tradeService.getRecentTrade(category.getCategoryId()))
                    .map(t -> t.getTradePrice())
                    .orElse(BigDecimal.ZERO);

            BigDecimal evaluateAmount = currentPrice.multiply(quantity);
            BigDecimal profit = evaluateAmount.subtract(buyAmount);

            totalBuyAmount = totalBuyAmount.add(buyAmount);
            totalEvaluateAmount = totalEvaluateAmount.add(evaluateAmount);

            // asset DTO 생성
            InvestAssetDto assetDto = new InvestAssetDto(
                    category.getCategoryId(),
                    category.getCategoryName(),
                    category.getSymbol(),
                    quantity,
                    avgBuyPrice,
                    buyAmount,
                    evaluateAmount,
                    profit
            );

            assets.add(assetDto);

            // 웹소켓 전송: 체결된 종목만
            Map<String, BigDecimal> coinData = new HashMap<>();
            coinData.put("evaluateAmount", evaluateAmount);
            coinData.put("profit", profit);
            messagingTemplate.convertAndSend("/topic/assets/" + member.getMemberId() + "/" + category.getCategoryId(), coinData);
        }

        // 보유 자산이 없으면 summary 0, 있으면 실제 합계 계산
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalProfitRate = BigDecimal.ZERO;

        if (hasHolding) {
            totalProfit = totalEvaluateAmount.subtract(totalBuyAmount);
            totalProfitRate = totalBuyAmount.compareTo(BigDecimal.ZERO) > 0 ?
                    totalProfit.divide(totalBuyAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                    BigDecimal.ZERO;
        }

        InvestSummaryDto summary = new InvestSummaryDto(
                totalBuyAmount,
                totalEvaluateAmount,
                totalProfit,
                totalProfitRate
        );

        // summary 웹소켓 전송 (항상 전송, 초기 호출은 0)
        Map<String, BigDecimal> summaryData = new HashMap<>();
        summaryData.put("totalBuyAmount", totalBuyAmount);
        summaryData.put("totalEvaluateAmount", totalEvaluateAmount);
        summaryData.put("totalProfit", totalProfit);
        summaryData.put("totalProfitRate", totalProfitRate);
        messagingTemplate.convertAndSend("/topic/summary/" + member.getMemberId(), summaryData);

        return new InvestPortfolioDto(summary, assets);
    }



    /**
     * 특정 종목(symbol) 수량 및 기본 정보 조회
     */
    public InvestQuantityDto getQuantityByCategoryId(Long categoryId) {
        Member member = memberService.getCurrentMember();

        Category category = categoryRepository.findById(categoryId).orElse(null);

        if (category == null) {
            return new InvestQuantityDto(categoryId, "", "", BigDecimal.ZERO);
        }

        BigDecimal quantity = investRepository.findTotalHoldingByMemberAndCategory(member, category);
        if (quantity == null) quantity = BigDecimal.ZERO;

        return new InvestQuantityDto(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getSymbol(),
                quantity
        );
    }
}
