package com.example.heartbit.repository;

import com.example.heartbit.domain.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    // 1. 특정 주문(OrderId)에 연관된 모든 체결 내역 조회
    List<Trade> findByBuyOrder_OrderIdOrSellOrder_OrderId(Long buyOrderId, Long sellOrderId);

    // 2. 종목별 최신 체결 내역 리스트 (getTradeList 용)
    //@Query("SELECT t FROM Trade t WHERE t.buyOrder.category.categoryId = :categoryId ORDER BY t.tradeTime DESC")
    List<Trade> findByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(Long categoryId, Pageable pageable);

    // 3. 종목별 최근 체결 1건 (getRecentTrade 용)
   // @Query("SELECT t FROM Trade t WHERE t.buyOrder.category.categoryId = :categoryId ORDER BY t.tradeTime DESC")
    Optional<Trade> findTop1ByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(Long categoryId);
    // 4. 내 거래 내역 조회 (마이페이지용)
    @Query("""
    SELECT t
    FROM Trade t
    JOIN t.buyOrder bo
    JOIN t.sellOrder so
    WHERE bo.member.memberId = :memberIdr
       OR so.member.memberId = :memberId
    ORDER BY t.tradeTime DESC
""")
    Page<Trade> findTradeByMemberId(@Param("memberId") Long memberId, Pageable pageable);


    // 5. 특정 카테고리의 15분 차트 데이터 조회 (getInitialCandles 용)
    @Query("SELECT t FROM Trade t WHERE t.buyOrder.category.categoryId = :categoryId AND t.tradeTime > :time ORDER BY t.tradeTime ASC")
    List<Trade> findTradesByCategoryIdAndTradeTimeAfter(@Param("categoryId") Long categoryId, @Param("time") LocalDateTime time);
    // 6. 9시 기준가 로드용 (특정 시점 이전의 마지막 체결가)
    // @Query("SELECT t FROM Trade t WHERE t.buyOrder.category.categoryId = :categoryId AND t.tradeTime < :dateTime ORDER BY t.tradeTime DESC")
    Optional<Trade> findTop1ByTradeTimeBeforeOrderByTradeTimeDesc(LocalDateTime dateTime);
}