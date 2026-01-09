package com.example.heartbit.repository;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Invest;
import com.example.heartbit.domain.Member;
import com.example.heartbit.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * InvestRepository
 * 투자 내역 조회용
 * 한 회원 + 한 종목 기준 보유 수량과 평균 매수 단가 계산
 */
public interface InvestRepository extends JpaRepository<Invest, Long>{

    // 회원 + 종목 체결 내역 조회
    List<Invest> findByMemberAndCategory(Member member, Category category);

    List<Invest> findByMember_MemberId(Long memberId);

    // 회원 + 종목 기준 보유 수량 합계
    @Query("""
        SELECT SUM(i.investCount) 
        FROM Invest i 
        WHERE i.member = :member
        AND i.category = :category
    """)
    BigDecimal findTotalHoldingByMemberAndCategory(
            @Param("member") Member member,
            @Param("category") Category category
    );

    // 회원 + 종목 기준 평균 매수 단가
    @Query("""
    SELECT
        SUM(i.investPrice * i.investCount) / SUM(i.investCount)
    FROM Invest i
    WHERE i.member = :member
    AND i.category = :category
    AND i.investCount > 0
    """)
    BigDecimal findAvgBuyPriceByMemberAndCategory(
            @Param("member") Member member,
            @Param("category") Category category
    );


    // 특정 Trade와 연관된 투자 내역 조회
    List<Invest> findByTrade(Trade trade);
}
