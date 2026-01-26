package com.example.heartbit.controller;

import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.MemberOpenOrderResponse;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.dto.order.OrderResponse;
import com.example.heartbit.service.OrderService;
import com.example.heartbit.service.TradeEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j   // ✅ 추가
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "주문 관리 API", description = "매수/매도 주문 및 내역 조회를 담당합니다.")
public class OrderController {

    private final OrderService orderService;
    private final TradeEngineService tradeEngineService;

    @Operation(summary = "신규 주문 생성")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request
    ) {
        // 서비스 결과 그대로 반환
        OrderResponse response = orderService.createOrder(request);

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "호가창 조회")
    @GetMapping("/orderbook")
    public ResponseEntity<List<OrderBookResponse>> orderBook(
            @RequestParam Long categoryId,
            @RequestParam OrderType orderType,
            @RequestParam(defaultValue = "30") int limit) {

        // 엔진에서 해당 종목의 호가창을 가져옴
        TradeEngineService.MatchingOrder engineBook = tradeEngineService.getMatchingOrder(categoryId);
        if (engineBook == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        // 엔진 메모리의 스냅샷을 반환
        List<OrderBookResponse> snapshot = engineBook.getSnapshot(orderType, limit);

        return ResponseEntity.ok(snapshot);
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
    public ResponseEntity<MemberOpenOrderResponse> getOpenMyOrders(
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(orderService.getOpenOrderByMember(memberId, page, size));
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
