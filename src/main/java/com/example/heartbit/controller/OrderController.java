package com.example.heartbit.controller;

import com.example.heartbit.dto.OrderRequest;
import com.example.heartbit.dto.OrderResponse;
import com.example.heartbit.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "주문 관리 API", description = "매수/매도 주문 및 내역 조회를 담당합니다.")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "신규 주문 생성", description = "종목에 대해 매수 또는 매도 주문을 넣습니다.")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "개인 주문 내역 조회", description = "특정 사용자가 넣은 모든 주문 내역을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(@RequestParam Long memberId) {
        List<OrderResponse> responses = orderService.getOrderByMember(memberId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "전체 취소", description = "아직 체결되지 않은 전체 주문들을 취소합니다.")
    @PatchMapping("/cancel-all")
    public ResponseEntity<Void> cancelAllOrders(@RequestParam Long memberId) {
        orderService.cancelAllOrders(memberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "주문 취소", description = "아직 체결되지 않은 주문을 취소합니다.")
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}
