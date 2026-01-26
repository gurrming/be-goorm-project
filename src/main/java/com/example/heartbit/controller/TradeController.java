package com.example.heartbit.controller;

import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trades")
@Tag(name= "체결 관리 API")
public class TradeController {

    private final TradeService tradeService;

    // 1) 종목별 최신 체결 리스트
    @Operation(summary = "종목별 최신 체결 리스트", description = "종목 ID를 통해 종목별 체결 리스트를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<TradeResponse>> tradeByCategory(
            @RequestParam Long categoryId, @RequestParam(defaultValue = "20") int limit) {
        List<TradeResponse> trades = tradeService.getTradeList(categoryId, limit);
        return ResponseEntity.ok(trades);
    }

    // 2) 종목별 최근 체결 1건 (현재가) - 웹소켓으로 실시간 데이터 뿌리기 전에 초기 데이터용
    @Operation(summary = "종목별 최근 체결 내역 1개", description = "종목 ID를 통해 종목별 가장 최근에 체결 된 내역 1건을 조회합니다.")
    @GetMapping("/recent")
    public ResponseEntity<TradeResponse> tradeRecent(@RequestParam Long categoryId) {
        TradeResponse recentTrade = tradeService.getRecentTrade(categoryId);
        return ResponseEntity.ok(recentTrade);
    }

    // 3) 특정 주문의 체결 내역
    @GetMapping("/order/{orderId}")
    @Operation(summary = "주문의 체결 내역 상태", description = "주문 ID를 통해 체결된 특정 내역의 현재 주문 상태를 조회합니다.")
    public ResponseEntity<List<TradeResponse>> tradeByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(tradeService.getTradeByOrder(orderId));
    }

    // 4) 내 체결 내역
    @GetMapping("/my")
    @Operation(summary = "개인 체결 내역", description = "멤버 ID를 통해 개인별 체결 리스트를 조회합니다.")
    public ResponseEntity<List<TradeResponse>> tradeMy(
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        //int adjustedPage = (page > 0) ? page - 1 : 0;
        // 서비스에서 Pageable을 사용하도록 수정했으므로 파라미터를 맞춰줍니다.
        return ResponseEntity.ok(tradeService.getMyTrade(memberId, page, size));
    }

    @Operation(summary = "차트 초기 데이터 조회(커서기반)", description = "마지막 데이터 ID를 기준으로 이전 체결 내역을 조회합니다.")
    @GetMapping("/chart")
    public ResponseEntity<List<Map<String, Object>>> getChartData(@RequestParam Long categoryId,
                                                                  @RequestParam(required = false) Long lastId,
                                                                  @RequestParam(defaultValue = "20") int size) {

        //int adjustedPage = (page > 0) ? page - 1 : 0;
        return ResponseEntity.ok(tradeService.getInitialCandles(categoryId, lastId, size));
    }

}

