package com.example.heartbit.repository;

import com.example.heartbit.domain.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByBuyOrder_OrderIdOrSellOrder_OrderId(Long buyOrderId, Long sellOrderId);
    Page<Trade> findAllByOrderByTradeTimeDesc(Pageable pageable);

    @Query("""
        SELECT t
        FROM Trade t
        JOIN t.buyOrder bo
        JOIN t.sellOrder so
        WHERE bo.member.memberId = :memberId
           OR so.member.memberId = :memberId
    """)
    Page<Trade> findTradeByMemberId(@Param("memberId") Long memberId, Pageable pageable);

}
