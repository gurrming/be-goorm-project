package com.example.heartbit.controller;

import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.dto.order.OrderResponse;
import com.example.heartbit.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j   // ✅ 추가
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "주문 관리 API", description = "매수/매도 주문 및 내역 조회를 담당합니다.")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "신규 주문 생성")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request
    ) {
        log.info(
                "CREATE ORDER | isBot={} | memberId={} | categoryId={} | type={} | price={} | count={}",
                request.getIsBot(),
                request.getMemberId(),
                request.getCategoryId(),
                request.getOrderType(),
                request.getOrderPrice(),
                request.getOrderCount()
        );

        // ✅ 서비스 결과 그대로 반환
        OrderResponse response = orderService.createOrder(request);

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "호가창 조회")
    @GetMapping("/orderbook")
    public ResponseEntity<List<OrderBookResponse>> orderBook(
            @RequestParam Long categoryId,
            @RequestParam OrderType orderType,
            Pageable pageable) {
        return ResponseEntity.ok(
                orderService.getOrderBook(categoryId, orderType, pageable.getPageSize())
        );
    }

    @Operation(summary = "회원 주문 조회")
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @RequestParam Long memberId
    ) {
        return ResponseEntity.ok(
                orderService.getOrderByMember(memberId)
        );
    }

    @Operation(summary = "회원 미체결 주문 조회")
    @GetMapping("/open")
    public ResponseEntity<List<OrderResponse>> getOpenMyOrders(
            @RequestParam Long memberId
    ) {
        return ResponseEntity.ok(orderService.getOpenOrderByMember(memberId));
    }

    @Operation(summary = "주문 취소")
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId
    ) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "전체 주문 취소")
    @PatchMapping("/cancel-all")
    public ResponseEntity<Void> cancelAllOrders(
            @RequestParam Long memberId
    ) {
        orderService.cancelAllOrders(memberId);
        return ResponseEntity.noContent().build();
    }
}
