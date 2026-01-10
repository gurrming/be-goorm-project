package com.example.heartbit.service;

import com.example.heartbit.domain.Invest;
import com.example.heartbit.dto.InvestResponse;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.repository.InvestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;


import com.example.heartbit.domain.Invest;
import com.example.heartbit.dto.InvestResponse;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.repository.InvestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestService {

    private final InvestRepository investRepository;
    private final TradeService tradeService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * [REST API] 사용자의 현재 투자 현황을 전체 조회 (초기 진입용)
     */
    @Transactional(readOnly = true)
    public InvestResponse getInvestSummary(Long memberId) {
        // 1. 해당 유저의 모든 보유 종목 가져오기
        List<Invest> investList = investRepository.findAllByMember_MemberId(memberId);

        // 2. 각 종목별 실시간 시세 반영 및 상세 계산
        List<InvestResponse.AssetDetailDto> assetList = investList.stream()
                .map(this::convertToAssetDetailDto)
                .collect(Collectors.toList());

        // 3. 상단 요약 정보(총합) 계산
        return buildInvestResponse(assetList);
    }

    /**
     * [WebSocket] 시세 변동 시 해당 종목 보유자들에게 실시간 자산 스냅샷 전송
     */
    @Transactional(readOnly = true)
    public void broadcastAssetUpdate(Long categoryId, BigDecimal newPrice) {
        // 1. 해당 종목(categoryId)을 보유한 유저 ID 목록 조회 (Distinct)
        List<Long> memberIds = investRepository.findMemberIdsByCategoryId(categoryId);

        if (memberIds.isEmpty()) return;

        // 2. 각 유저별로 전체 자산 현황을 재계산해서 웹소켓 전송
        for (Long memberId : memberIds) {
            try {
                InvestResponse totalSummary = getInvestSummary(memberId);

                // 개인용 채널로 전송 (/topic/asset/1, /topic/asset/2 ...)
                messagingTemplate.convertAndSend("/topic/invest/" + memberId, totalSummary);

                log.debug("실시간 자산 업데이트 전송 - MemberID: {}, CategoryID: {}", memberId, categoryId);
            } catch (Exception e) {
                log.error("자산 업데이트 전송 실패 - MemberID: {}", memberId, e);
            }
        }
    }

    /**
     * 개별 종목 계산 로직 (DTO 변환)
     */
    private InvestResponse.AssetDetailDto convertToAssetDetailDto(Invest invest) {
        Long categoryId = invest.getCategory().getCategoryId();

        // TradeService에서 최신가 가져오기
        TradeResponse trade = (TradeResponse) tradeService.getCurrentTrade(categoryId);
        BigDecimal currentPrice = (trade != null) ? trade.getTradePrice() : BigDecimal.ZERO;

        BigDecimal quantity = invest.getInvestCount();
        BigDecimal avgPrice = invest.getInvestPrice();

        BigDecimal buyAmount = quantity.multiply(avgPrice);       // 매수금액
        BigDecimal evalAmount = quantity.multiply(currentPrice); // 평가금액
        BigDecimal profit = evalAmount.subtract(buyAmount);      // 평가손익
        BigDecimal profitRate = calculateProfitRate(buyAmount, evalAmount); // 수익률

        return InvestResponse.AssetDetailDto.builder()
                .categoryName(invest.getCategory().getCategoryName())
                .symbol(invest.getTrade().getSymbol())
                .investCount(quantity)
                .avgPrice(avgPrice)
                .buyAmount(buyAmount)
                .currentPrice(currentPrice)
                .evaluationAmount(evalAmount)
                .evaluationProfit(profit)
                .profitRate(profitRate)
                .build();
    }

    /**
     * 전체 요약 정보 빌드
     */
    private InvestResponse buildInvestResponse(List<InvestResponse.AssetDetailDto> assetList) {
        BigDecimal totalBuy = assetList.stream()
                .map(InvestResponse.AssetDetailDto::getBuyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEval = assetList.stream()
                .map(InvestResponse.AssetDetailDto::getEvaluationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfit = totalEval.subtract(totalBuy);
        BigDecimal totalProfitRate = calculateProfitRate(totalBuy, totalEval);

        return InvestResponse.builder()
                .totalBuyAmount(totalBuy)
                .totalEvaluation(totalEval)
                .totalProfit(totalProfit)
                .totalProfitRate(totalProfitRate)
                .assetList(assetList)
                .build();
    }

    /**
     * 수익률 계산 (소수점 4자리 반올림)
     */
    private BigDecimal calculateProfitRate(BigDecimal buy, BigDecimal eval) {
        if (buy == null || buy.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return eval.subtract(buy)
                .divide(buy, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}