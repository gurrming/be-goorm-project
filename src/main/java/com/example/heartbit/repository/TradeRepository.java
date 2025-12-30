package com.example.heartbit.repository;

import com.example.heartbit.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByTradeBuyIdOrTradeSellId(Long buyOrderId, Long sellOrderId);
    List<Trade> findByTradeTime();

    @Query("""
        SELECT t
        FROM Trade t
        JOIN t.buyOrder bo
        JOIN t.sellOrder so
        WHERE bo.member = :memberId
           OR so.member = :memberId
        ORDER BY t.tradeTime DESC
    """)
    List<Trade> findTradeByMemberId(@Param("member") Long memberId);


}
