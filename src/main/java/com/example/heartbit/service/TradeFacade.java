package com.example.heartbit.service;

import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.service.InvestService;
import com.example.heartbit.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeFacade {
    private final TradeService tradeService;
    private final InvestService investService;

    /**
     * 1. 체결 처리 (TradeService)
     * 2. 변동된 가격으로 자산 알림 (InvestService)
     */
    @Transactional
    public void handleTradeAndNotify(Long categoryId, List<TradeResponse> tradeResults) {
        // 1. 시세 업데이트 및 DB 저장
        tradeService.processTradeResults(categoryId, tradeResults);

        // 2. 업데이트된 최신 가격 확인
        BigDecimal currentPrice = tradeService.getCurrentPrice(categoryId);

        // 3. 해당 가격을 들고 자산 서비스에 알림 요청
        investService.broadcastAssetUpdate(categoryId, currentPrice);
    }
}