package com.example.heartbit.repository;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderStatus;
import com.example.heartbit.domain.OrderType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 회원의 전체 주문 내역 (최신순)
    List<Order> findByMember_MemberIdOrderByOrderTimeDesc(Long memberId);

    // 회원의 미체결 주문 내역
    Slice<Order> findByMember_MemberIdAndOrderStatusInOrderByOrderTimeDesc(
            Long memberId, Collection<OrderStatus> statuses,  Pageable pageable
    );

    // 특정 회원의 미체결(OPEN, PARTIAL) 주문 수량 총합
    @Query("""
        SELECT COUNT(o) FROM Order o 
        WHERE o.member.memberId = :memberId 
        AND o.orderStatus IN :statuses
        """)
    Long countOpenOrdersByMember(Long memberId, List<OrderStatus> statuses);

    // 체결되지 않은 주문들 시간 순으로 가져온 내역
    List<Order> findByOrderStatusInOrderByOrderTimeAsc(List<OrderStatus> statuses);


}

