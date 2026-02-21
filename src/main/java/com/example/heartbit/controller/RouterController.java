package com.example.heartbit.controller;

import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sharding")
public class RouterController {
    private final OrderRouter orderRouter;

    @PostMapping("/route")
    public ResponseEntity<ApiResponse<String>> routeOrder(@RequestBody OrderRequest request) {
        orderRouter.route(request);
        return ResponseEntity.ok(ApiResponse.onSuccess("주문이 정상적으로 접수되어 라우팅되었습니다."));
    }
}