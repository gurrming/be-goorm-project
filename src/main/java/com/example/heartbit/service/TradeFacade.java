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
        // 1. 시세 업데이트 및 DB 저장 (TradeService 담당)
        tradeService.processTradeResults(categoryId, tradeResults);

        // 2. 업데이트된 해당 종목의 최신 가격 확인
        // (주의: getAllCurrentPrices는 Map을 반환하므로, 특정 ID의 값만 가져오도록 수정)
        BigDecimal currentPrice = tradeService.getCurrentPrice(categoryId);

        // 3. 해당 가격을 인자로 전달하여 자산 서비스 알림 요청 (InvestService 담당)
        // 이제 InvestService는 내부에서 TradeService를 호출하지 않으므로 순환 참조가 없습니다.
        investService.broadcastAssetUpdate(categoryId, currentPrice);
    }
}