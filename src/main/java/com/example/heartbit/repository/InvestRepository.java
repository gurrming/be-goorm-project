package com.example.heartbit.repository;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Invest;
import com.example.heartbit.domain.Member;
import com.example.heartbit.domain.Trade;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * InvestRepository
 * 투자 내역 조회용
 * 한 회원 + 한 종목 기준 보유 수량과 평균 매수 단가 계산
 */
public interface InvestRepository extends JpaRepository<Invest, Long> {

    List<Invest> findAllByMember_MemberId(Long memberId);

    Slice<Invest> findAllByMember_MemberId(Long memberId, Pageable pageable);

    @Query("SELECT DISTINCT i.member.memberId FROM Invest i WHERE i.category.categoryId = :categoryId")
    List<Long> findMemberIdsByCategoryId(@Param("categoryId") Long categoryId);

    Optional<Invest> findByMember_MemberIdAndCategory_CategoryId(Long memberId, Long categoryId);

    List<Invest> findByCategory_CategoryId(Long categoryId);
}