package com.example.heartbit.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberOpenOrderResponse {
    // 페이징 처리된 주문 목록
    private Slice<OrderResponse> orders;

    // 전체 미체결 주문의 개수
    private Long totalOpenOrderCount;
}
