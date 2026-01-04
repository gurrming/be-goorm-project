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

    // [유지] 특정 주문(OrderId)에 연관된 모든 체결 내역 조회
    List<Trade> findByBuyOrder_OrderIdOrSellOrder_OrderId(Long buyOrderId, Long sellOrderId);

    // [보완] 특정 카테고리(categoryId)의 최신 체결 내역 리스트 (getTradeList 용)
    // Pageable을 사용하면 Service에서 limit 갯수를 조절하기 매우 편합니다.
    @Query("SELECT t FROM Trade t WHERE t.buyOrder.category.categoryId = :categoryId ORDER BY t.tradeTime DESC")
    List<Trade> findTopTradesByCategory(@Param("categoryId") Long categoryId, Pageable pageable);

    // [추가] 특정 카테고리의 가장 최근 체결 1건 (getRecentTrade 용)
    // 9시 기준가 로드와 별개로, '현재가' 표시를 위해 필요합니다.
    @Query("SELECT t FROM Trade t WHERE t.buyOrder.category.categoryId = :categoryId ORDER BY t.tradeTime DESC")
    Optional<Trade> findTop1ByCategoryOrderByTradeTimeDesc(@Param("categoryId") Long categoryId);

    // [유지] 내 거래 내역 조회 (마이페이지용)
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

    // [유지] 9시 기준가 로드용 (가장 중요한 메서드)
    Optional<Trade> findTop1ByTradeTimeBeforeOrderByTradeTimeDesc(LocalDateTime dateTime);

    List<Trade> findByTradeTimeAfterOrderByTradeTimeAsc(LocalDateTime dateTime);

    // [정리 대상] findAllByOrderByTradeTimeDesc
    // 특정 종목(카테고리) 구분 없이 전 종목의 체결을 가져오므로, 단일 종목 거래소라면 유지해도 무방하나
    // 멀티 종목(비트코인, 이더리움 등)이라면 위 findTopTradesByCategory로 대체하는 것이 좋습니다.
}