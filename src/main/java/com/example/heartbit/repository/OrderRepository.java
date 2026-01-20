package com.example.heartbit.repository;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderStatus;
import com.example.heartbit.domain.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 회원의 전체 주문 내역 (최신순)
    List<Order> findByMember_MemberIdOrderByOrderTimeDesc(Long memberId);

    // 회원의 미체결 주문 내역
    List<Order> findByMember_MemberIdAndOrderStatusInOrderByOrderTimeDesc(
            Long memberId, Collection<OrderStatus> statuses
    );

    // 만료된 주문 조회 (스케줄러용)
    @Query("""
        SELECT o FROM Order o 
        WHERE o.orderTime <= :expirationTime 
        AND (o.orderStatus = 'OPEN' OR o.orderStatus = 'PARTIAL')
        """)
    List<Order> findExpiredOrders(@Param("expirationTime") LocalDateTime expirationTime);


    List<Order> findByCategory_CategoryIdAndOrderTypeAndOrderStatusIn(
            Long categoryId, OrderType orderType, List<OrderStatus> activeStatuses);
}

