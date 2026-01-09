package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.repository.InvestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class TradeSetService {

    private final InvestRepository investRepository;

    /**
     * 체결 결과를 보유 자산에 반영
     */
    public void settleTrade(Trade trade) {

        BigDecimal tradeCount = trade.getTradeCount();
        BigDecimal tradePrice = trade.getTradePrice();
        Category category = trade.getBuyOrder().getCategory();

        // 매수자
        Member buyer = trade.getBuyOrder().getMember();
        saveInvest(buyer, category, trade, tradeCount, tradePrice);

        // 매도자
        Member seller = trade.getSellOrder().getMember();
        saveInvest(seller, category, trade, tradeCount.negate(), tradePrice);
    }

    /**
     * Invest 생성
     */
    private void saveInvest(Member member,
                            Category category,
                            Trade trade,
                            BigDecimal count,
                            BigDecimal price) {

        // 수량 0이면 저장 의미 없음
        if (count.compareTo(BigDecimal.ZERO) == 0) return;

        Invest invest = new Invest(
                member,
                category,
                trade,
                count,
                price
        );

        investRepository.save(invest);
    }
}
