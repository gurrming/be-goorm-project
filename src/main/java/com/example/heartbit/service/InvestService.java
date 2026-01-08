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
    private final SimpMessagingTemplate messagingTemplate; // 웹소켓

    /**
     * 전체 포트폴리오 조회
     */
    public InvestPortfolioDto getPortfolio() {

        Member member = memberService.getCurrentMember();
        BigDecimal totalBuyAmount = BigDecimal.ZERO;
        BigDecimal totalEvaluateAmount = BigDecimal.ZERO;
        List<InvestAssetDto> assets = new ArrayList<>();
        List<Category> categories = categoryRepository.findAll();

        for (Category category : categories) {

            BigDecimal quantity = investRepository.findTotalHoldingByMemberAndCategory(member, category);
            if (quantity == null) quantity = BigDecimal.ZERO;  // null -> 0 처리

            // 종목 수량이 0이면 건너뜀 (웹소켓 전송 X)
            if (quantity.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal avgBuyPrice = investRepository.findAvgBuyPriceByMemberAndCategory(member, category);
            if (avgBuyPrice == null) avgBuyPrice = BigDecimal.ZERO;

            BigDecimal buyAmount = avgBuyPrice.multiply(quantity);

            BigDecimal currentPrice = Optional.ofNullable(tradeService.getRecentTrade(category.getCategoryId()))
                    .map(t -> t.getTradePrice())
                    .orElse(BigDecimal.ZERO);

            BigDecimal evaluateAmount = currentPrice.multiply(quantity);
            BigDecimal profit = evaluateAmount.subtract(buyAmount);

            totalBuyAmount = totalBuyAmount.add(buyAmount);
            totalEvaluateAmount = totalEvaluateAmount.add(evaluateAmount);

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

            // 종목 웹소켓 전송 (quantity > 0일 때)
            Map<String, BigDecimal> coinData = new HashMap<>();
            coinData.put("quantity", quantity);
            coinData.put("evaluateAmount", evaluateAmount);
            coinData.put("profit", profit);
            messagingTemplate.convertAndSend("/topic/assets/" + member.getMemberId(), coinData);
        }

        // 총합(summary) 계산
        BigDecimal totalProfit = totalEvaluateAmount.subtract(totalBuyAmount);
        BigDecimal totalProfitRate = totalBuyAmount.compareTo(BigDecimal.ZERO) > 0 ?
                totalProfit.divide(totalBuyAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                BigDecimal.ZERO;

        InvestSummaryDto summary = new InvestSummaryDto(
                totalBuyAmount,
                totalEvaluateAmount,
                totalProfit,
                totalProfitRate
        );

        // summary 웹소켓 전송
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
            return new InvestQuantityDto(null, null, null, BigDecimal.ZERO);
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
