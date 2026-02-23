package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.dto.trade.PriceChangedEvent;
import com.example.heartbit.dto.trade.TradesCompletedEvent;
import com.example.heartbit.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import com.example.heartbit.domain.Invest;
import com.example.heartbit.dto.InvestResponse;
import com.example.heartbit.repository.InvestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
public class InvestService {

    private final InvestRepository investRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final TradeService tradeService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AssetRepository assetRepository;

    public InvestService(InvestRepository investRepository,
                         MemberRepository memberRepository,
                         CategoryRepository categoryRepository,
                         SimpMessagingTemplate messagingTemplate,
                         @Lazy TradeService tradeService,
                            StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper,
                         AssetRepository assetRepository) {
        this.investRepository = investRepository;
        this.memberRepository = memberRepository;
        this.categoryRepository = categoryRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.tradeService = tradeService;
        this.assetRepository = assetRepository;
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

        Asset asset = assetRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("자산 정보를 찾을 수 없습니다."));

        BigDecimal currentCash = asset.getAssetCash();
        // 총 자산 = 현금 + 코인 총 평가금액
        BigDecimal totalAsset = currentCash.add(totalEvaluation);

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
                .hasNext(pagedInvests.hasNext())
                .assetCash(currentCash)
                .totalAsset(totalAsset)
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
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTradesCompleted(TradesCompletedEvent event) {
        Long categoryId = event.getCategoryId();

        // 넘어온 체결 데이터 리스트를 순회하며 매수/매도자의 자산을 업데이트합니다.
        for (TradesCompletedEvent.TradeDetail detail : event.getTradeDetails()) {

            // 1. 매수자 자산 업데이트 (평단가 계산 등)
            if (detail.getBuyerId() != null) {
                processInvestLogic(detail.getBuyerId(), categoryId, detail.getTradeCount(), detail.getTradePrice(), "BUY", detail.getTradeId());
            }

            // 2. 매도자 자산 업데이트 (수량 차감 등)
            if (detail.getSellerId() != null) {
                processInvestLogic(detail.getSellerId(), categoryId, detail.getTradeCount(), detail.getTradePrice(), "SELL", detail.getTradeId());
            }
        }
    }

    /**
     * [핵심 자산 업데이트 로직]
     * 기존 saveOrUpdateInvest의 비즈니스 로직(물타기, 차감, 삭제)을 그대로
     * 단, Trade 객체 대신 tradeId만 저장하여 영속성 문제를 해결
     */
    private void processInvestLogic(Long memberId, Long categoryId, BigDecimal tradeCount, BigDecimal tradePrice, String type, Long tradeId) {

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

        // 💡 핵심 변경 포인트: 객체 세팅 대신 ID만 세팅!
        invest.setTradeId(tradeId);

        BigDecimal currentCount = invest.getInvestCount() == null ? BigDecimal.ZERO : invest.getInvestCount();
        BigDecimal currentAvg = invest.getInvestPrice() == null ? BigDecimal.ZERO : invest.getInvestPrice();

        if ("BUY".equals(type)) {
            // [매수] 평단가 물타기 계산
            BigDecimal oldTotal = currentCount.multiply(currentAvg);
            BigDecimal newTotal = tradeCount.multiply(tradePrice);
            BigDecimal totalCount = currentCount.add(tradeCount);

            if (totalCount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal newAvg = oldTotal.add(newTotal).divide(totalCount, 8, RoundingMode.HALF_UP);
                invest.setInvestPrice(newAvg);
            }
            invest.setInvestCount(totalCount);

        } else {
            // [매도] 수량 차감
            BigDecimal resultCount = currentCount.subtract(tradeCount);
            invest.setInvestCount(resultCount);
        }

        // 수량이 0 이하면 DB에서 삭제, 남았으면 저장 (더티 체킹 또는 save)
        if (invest.getInvestCount().compareTo(BigDecimal.ZERO) <= 0) {
            if (invest.getInvestId() != null) {
                investRepository.delete(invest);
            }
        } else {
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

                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("memberId", memberId);
                messageMap.put("totalSummary", totalSummary);

                stringRedisTemplate.convertAndSend("ws-invest-channel", objectMapper.writeValueAsString(messageMap));

                log.debug("실시간 자산 업데이트 전송 - MemberID: {}, CategoryID: {}", memberId, categoryId);
            } catch (Exception e) {
                log.error("자산 업데이트 전송 실패 - MemberID: {}", memberId, e);
            }
        }
    }





}