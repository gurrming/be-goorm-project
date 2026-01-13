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
import java.util.Map;
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
    /**
     * [REST API] 사용자의 현재 투자 현황을 전체 조회
     * 컨트롤러에서 조회한 실시간 시세 맵을 인자로 받습니다.
     */
    @Transactional(readOnly = true)
    public InvestResponse getInvestSummary(Long memberId, Map<Long, BigDecimal> priceMap) {
        // 1. 해당 유저의 모든 보유 종목 가져오기
        List<Invest> investList = investRepository.findAllByMember_MemberId(memberId);

        // 2. 각 종목별 실시간 시세 반영 및 상세 계산
        List<InvestResponse.AssetDetailDto> assetList = investList.stream()
                .map(invest -> {
                    // 해당 종목의 실시간 가격을 맵에서 추출 (없으면 0)
                    BigDecimal currentPrice = priceMap.getOrDefault(
                            invest.getCategory().getCategoryId(),
                            BigDecimal.ZERO
                    );
                    // 인자 2개를 명시적으로 전달하여 호출 (에러 해결 지점)
                    return convertToAssetDetailDto(invest, currentPrice);
                })
                .collect(Collectors.toList());

        // 3. 상단 요약 정보(총합) 계산
        return buildInvestResponse(assetList);
    }

    /**
     * [WebSocket] 시세 변동 시 해당 종목 보유자들에게 실시간 자산 스냅샷 전송
     */
    public void broadcastAssetUpdate(Long categoryId, BigDecimal newPrice) {
        List<Long> memberIds = investRepository.findMemberIdsByCategoryId(categoryId);
        if (memberIds.isEmpty()) return;

        for (Long memberId : memberIds) {
            // 이 유저의 다른 보유 종목들 시세는 일단 무시하고,
            // 현재 변동된 종목의 가격만 업데이트해서 보내는 로직으로 구성하거나
            // 전체 요약을 다시 불러올 때 가격 Map을 활용합니다.
            InvestResponse totalSummary = getInvestSummarySimple(memberId, categoryId, newPrice);
            messagingTemplate.convertAndSend("/topic/invest/" + memberId, totalSummary);
        }
    }

    /**
     * 개별 종목 계산 로직 (DTO 변환)
     */
    private InvestResponse.AssetDetailDto convertToAssetDetailDto(Invest invest, BigDecimal currentPrice) {
        // 1. 기본 정보 추출
        BigDecimal quantity = invest.getInvestCount(); // 보유 수량
        BigDecimal avgPrice = invest.getInvestPrice();  // 평단가

        // 2. 금액 계산
        BigDecimal buyAmount = quantity.multiply(avgPrice);       // 매수금액 = 수량 * 평단가
        BigDecimal evalAmount = quantity.multiply(currentPrice); // 평가금액 = 수량 * 현재가
        BigDecimal profit = evalAmount.subtract(buyAmount);      // 평가손익 = 평가금액 - 매수금액

        // 3. 수익률 계산
        BigDecimal profitRate = calculateProfitRate(buyAmount, evalAmount);

        // 4. DTO 빌드
        return InvestResponse.AssetDetailDto.builder()
                .categoryName(invest.getCategory().getCategoryName())
                .symbol(invest.getCategory().getSymbol()) // invest.getTrade().getSymbol() 대신 category 권장
                .investCount(quantity)
                .avgPrice(avgPrice)
                .buyAmount(buyAmount)
                .currentPrice(currentPrice)
                .evaluationAmount(evalAmount)
                .evaluationProfit(profit)
                .profitRate(profitRate)
                .build();
    }

    private InvestResponse getInvestSummarySimple(Long memberId, Long categoryId, BigDecimal newPrice) {
        List<Invest> investList = investRepository.findAllByMember_MemberId(memberId);

        List<InvestResponse.AssetDetailDto> assetList = investList.stream()
                .map(invest -> {
                    // 변동된 종목(categoryId)이면 newPrice 사용, 아니면 기존 DB의 평단가 등을 활용
                    // (더 정확하려면 다른 종목의 시세도 필요하지만, 성능상 변동된 종목만 우선 반영)
                    BigDecimal price = invest.getCategory().getCategoryId().equals(categoryId)
                            ? newPrice
                            : invest.getInvestPrice(); // 다른 종목은 평단가로 임시 계산하거나 0 처리
                    return convertToAssetDetailDto(invest, price);
                })
                .collect(Collectors.toList());

        return buildInvestResponse(assetList);
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