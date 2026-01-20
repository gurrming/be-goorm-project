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


    @Query("SELECT t FROM Trade t " +
    "WHERE t.buyOrder.category.categoryId = :categoryID " +
    "ORDER BY t.tradeId DESC")
    List<Trade> findLatestTrades(@Param("categoryID") Long categoryID, Pageable pageable);

    @Query("SELECT t FROM Trade t " +
    "WHERE t.buyOrder.category.categoryId = :categoryID " +
    "AND t.tradeId < :lastId " +
    "ORDER BY t.tradeId DESC")
    List<Trade> findTradesByCursor(
    @Param("categoryID") Long categoryID,
    @Param("lastId") Long lastId,
    Pageable pageable);

    // 1. 특정 주문(OrderId)에 연관된 모든 체결 내역 조회
    List<Trade> findByBuyOrder_OrderIdOrSellOrder_OrderId(Long buyOrderId, Long sellOrderId);


    // 3. 종목별 최근 체결 1건 (getRecentTrade 용)
   // @Query("SELECT t FROM Trade t WHERE t.buyOrder.category.categoryId = :categoryId ORDER BY t.tradeTime DESC")
    Optional<Trade> findTop1ByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(Long categoryId);
    // 4. 내 거래 내역 조회 (마이페이지용)
    @Query("""
    SELECT t
    FROM Trade t
    JOIN t.buyOrder bo
    JOIN t.sellOrder so
    WHERE bo.member.memberId = :memberId
       OR so.member.memberId = :memberId
    ORDER BY t.tradeTime DESC
""")
    Page<Trade> findTradeByMemberId(@Param("memberId") Long memberId, Pageable pageable);


    // TradeRepository.java
    // 최신순으로 정렬하여 페이징 처리
    Page<Trade> findByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(Long categoryId, Pageable pageable);



        // 6. 9시 기준가 로드용 (특정 종목의 특정 시점 이전 마지막 체결가)
        // 파라미터에 categoryId를 추가하여 해당 종목의 전일 종가를 정확히 가져오도록 합니다.
        @Query("SELECT t FROM Trade t WHERE t.buyOrder.category.categoryId = :categoryId " +
                "AND t.tradeTime < :dateTime ORDER BY t.tradeTime DESC LIMIT 1")
        Optional<Trade> findTop1ByCategoryIdAndTradeTimeBefore(
                @Param("categoryId") Long categoryId,
                @Param("dateTime") LocalDateTime dateTime);

    // @Query("SELECT t FROM Trade t WHERE t.buyOrder.category.categoryId = :categoryId AND t.tradeTime < :dateTime ORDER BY t.tradeTime DESC")
    Optional<Trade> findTop1ByTradeTimeBeforeOrderByTradeTimeDesc(LocalDateTime dateTime);

    @Query("SELECT t FROM Trade t " +
            "WHERE t.buyOrder.category.categoryId = :categoryId " +
            "AND t.tradeTime >= :afterTime " +
            "ORDER BY t.tradeTime ASC")
    List<Trade> findTradesByCategoryIdAndTradeTimeAfter(
            @Param("categoryId") Long categoryId,
            @Param("afterTime") LocalDateTime afterTime
    );
}