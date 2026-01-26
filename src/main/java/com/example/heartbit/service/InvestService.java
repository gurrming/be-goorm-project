package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.dto.*;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.InvestRepository;
import com.example.heartbit.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
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
public class InvestService {

    private final InvestRepository investRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TradeService tradeService;

    public InvestService(InvestRepository investRepository,
                         MemberRepository memberRepository,
                         CategoryRepository categoryRepository,
                         SimpMessagingTemplate messagingTemplate,
                         @Lazy TradeService tradeService) { // ★ 여기에 @Lazy 추가 ★
        this.investRepository = investRepository;
        this.memberRepository = memberRepository;
        this.categoryRepository = categoryRepository;
        this.messagingTemplate = messagingTemplate;
        this.tradeService = tradeService;
    }

    /**
     * 보유 자산 요약 및 목록 조회 (무한 스크롤 적용)
     * - 상단 요약(합계): 페이징 없이 전체 보유 코인 기준으로 계산 (스크롤 할 때마다 최신 시세 반영)
     * - 하단 목록(리스트): 요청된 페이지(Slice)만큼만 반환
     */
    @Transactional(readOnly = true)
    public InvestResponse getInvestSummary(Long memberId, Pageable pageable) {

        // 1. [전체 요약용] 페이징 없이 해당 유저의 '모든' 투자 내역 조회
        List<Invest> allInvests = investRepository.findAllByMember_MemberId(memberId);

        // 2. [목록 표시용] 페이징 적용된 투자 내역 조회 (Slice 사용)
        Slice<Invest> pagedInvests = investRepository.findAllByMember_MemberId(memberId, pageable);


        // --- A. 전체 자산 합계 계산 (allInvests 사용) ---
        BigDecimal totalBuyAmount = BigDecimal.ZERO;    // 총 매수금액
        BigDecimal totalEvaluation = BigDecimal.ZERO;   // 총 평가금액

        for (Invest invest : allInvests) {
            // 현재가 조회 (TradeService -> Redis/DB)
            BigDecimal currentPrice = tradeService.getCurrentTrade(invest.getCategory().getCategoryId())
                    .getTradePrice();

            // 개별 종목의 매수금액 = 평단가 * 보유수량
            BigDecimal buyAmt = invest.getInvestPrice().multiply(invest.getInvestCount());
            // 개별 종목의 평가금액 = 현재가 * 보유수량
            BigDecimal evalAmt = currentPrice.multiply(invest.getInvestCount());

            totalBuyAmount = totalBuyAmount.add(buyAmt);
            totalEvaluation = totalEvaluation.add(evalAmt);
        }

        // 전체 평가손익 = 총 평가금액 - 총 매수금액
        BigDecimal totalProfit = totalEvaluation.subtract(totalBuyAmount);

        // 전체 수익률 = (총 평가손익 / 총 매수금액) * 100
        BigDecimal totalProfitRate = (totalBuyAmount.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ZERO :
                totalProfit.divide(totalBuyAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));


        // --- B. 리스트 변환 (pagedInvests 사용) ---
        List<InvestResponse.AssetDetailDto> assetList = pagedInvests.stream()
                .map(invest -> {
                    // 현재가 조회
                    BigDecimal currentPrice = tradeService.getCurrentTrade(invest.getCategory().getCategoryId())
                            .getTradePrice();

                    // 개별 계산
                    BigDecimal buyAmount = invest.getInvestPrice().multiply(invest.getInvestCount()); // 매수금액
                    BigDecimal evaluationAmount = currentPrice.multiply(invest.getInvestCount());     // 평가금액
                    BigDecimal evaluationProfit = evaluationAmount.subtract(buyAmount);               // 평가손익

                    // 개별 수익률
                    BigDecimal profitRate = (buyAmount.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ZERO :
                            evaluationProfit.divide(buyAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

                    // DTO 매핑
                    return InvestResponse.AssetDetailDto.builder()
                            .categoryId(invest.getCategory().getCategoryId())
                            .categoryName(invest.getCategory().getCategoryName())
                            .symbol(invest.getCategory().getSymbol())
                            .investCount(invest.getInvestCount())
                            .avgPrice(invest.getInvestPrice()) // 평단가
                            .buyAmount(buyAmount)
                            .currentPrice(currentPrice)
                            .evaluationAmount(evaluationAmount)
                            .evaluationProfit(evaluationProfit)
                            .profitRate(profitRate)
                            .build();
                })
                .collect(Collectors.toList());

        // 3. 최종 응답 빌드
        return InvestResponse.builder()
                .totalBuyAmount(totalBuyAmount)
                .totalEvaluation(totalEvaluation)
                .totalProfit(totalProfit)
                .totalProfitRate(totalProfitRate)
                .assetList(assetList)
                .hasNext(pagedInvests.hasNext()) // 다음 페이지 존재 여부 (무한 스크롤용)
                .build();
    }


    @EventListener
    public void handlePriceChange(PriceChangedEvent event) {

        broadcastAssetUpdate(event.getCategoryId(), event.getNewPrice());
    }

    /**
     * [저장 핵심 로직]
     * TradeService에서 체결 시 호출됨.
     * 매수 시 평단가를 섞고, 매도 시 수량을 뺌
     */
    @Transactional
    public void saveOrUpdateInvest(Long memberId, Trade trade, Long categoryId, BigDecimal tradeCount, BigDecimal tradePrice, String type) {

        // 1. 내 투자 내역 조회 (없으면 0으로 초기화된 객체 생성)
        Invest invest = investRepository.findByMember_MemberIdAndCategory_CategoryId(memberId, categoryId)
                .orElseGet(() -> {
                    Member member = memberRepository.getReferenceById(memberId);
                    Category category = categoryRepository.getReferenceById(categoryId);
                    return Invest.builder()
                            .member(member)
                            .category(category)
                            .investCount(BigDecimal.ZERO)
                            .investPrice(BigDecimal.ZERO)
                            .build();
                });

        // 엔티티에 nullable = false가 걸려있어서 이 줄이 없으면 에러가 납니다.
        invest.setTrade(trade);

        // 2. Null 방어 로직
        BigDecimal currentCount = invest.getInvestCount() == null ? BigDecimal.ZERO : invest.getInvestCount();
        BigDecimal currentAvg = invest.getInvestPrice() == null ? BigDecimal.ZERO : invest.getInvestPrice();

        if ("BUY".equals(type)) {
            // [매수] 평단가 물타기 계산
            BigDecimal oldTotal = currentCount.multiply(currentAvg); // 기존 총액
            BigDecimal newTotal = tradeCount.multiply(tradePrice);   // 신규 매수 총액
            BigDecimal totalCount = currentCount.add(tradeCount);    // 합친 수량

            // 0으로 나누기 에러 방지
            if (totalCount.compareTo(BigDecimal.ZERO) > 0) {
                // 새로운 평단가 = (기존총액 + 신규총액) / 전체수량
                BigDecimal newAvg = oldTotal.add(newTotal).divide(totalCount, 8, RoundingMode.HALF_UP);
                invest.setInvestPrice(newAvg);
            }
            invest.setInvestCount(totalCount);

        } else {
            // [매도] 수량만 빼기 (평단가는 변하지 않음)
            BigDecimal resultCount = currentCount.subtract(tradeCount);
            invest.setInvestCount(resultCount);
        }

        // 3. 수량이 0 이하면 DB에서 삭제, 남았으면 저장
        if (invest.getInvestCount().compareTo(BigDecimal.ZERO) <= 0) {
            if (invest.getInvestId() != null) {
                // 수량이 0이 되어 삭제할 때는 Trade 참조가 필요 없으므로 바로 삭제
                investRepository.delete(invest);
            }
        } else {
            // Trade가 set 되어 있으므로 정상적으로 저장됨
            investRepository.save(invest);
        }
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
                InvestResponse totalSummary = getInvestSummary(memberId, Pageable.unpaged());

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
                .categoryId(categoryId)
                .buyAmount(buyAmount)
                .currentPrice(currentPrice)
                .evaluationAmount(evalAmount)
                .evaluationProfit(profit)
                .profitRate(profitRate)
                .build();
    }

    /**
     * 전체 요약 정보 빌드(개별 종목 요약한거 합산)
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