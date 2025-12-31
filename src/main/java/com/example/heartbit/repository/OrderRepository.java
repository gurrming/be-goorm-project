package com.example.heartbit.repository;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderStatus;
import com.example.heartbit.domain.OrderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 미체결 주문 내역들(OPEN, PARTIAL)
    List<Order> findByMember_MemberIdAndOrderStatusIn(Long memberId, Collection<OrderStatus> statuses);

    // 특정 종목의 매수/매도 현황 조회(호가 테이블 생성시)
    List<Order> findByCategory_CategoryIdAndOrderTypeAndOrderStatusIn(
            Long categoryId, OrderType type, Collection<OrderStatus> statuses
    );

    // 체결 완료된 최근 내역들(FILLED - 시간 기준 최신순으로)
    Page<Order> findByMember_MemberIdAndOrderStatusOrderByOrdersTimeDesc(
            Long memberId, OrderStatus status, Pageable pageable
    );

}