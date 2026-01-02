package com.example.heartbit.service;

import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.repository.TradeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TradeService {
    private final TradeRepository tradeRepository;

    public TradeService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    // 체결된 전체 목록 20개
    public List<TradeResponse> getTradeList(Long categoryId, int limit) {
        // 최신순(Desc)으로 정렬하고, 0페이지부터 limit(20)개만큼 가져오라는 설정
        Pageable pageable = PageRequest.of(0, limit, Sort.by("tradeTime").descending());
        return tradeRepository.findTopByCategory_CategoryIdOrderByTradeTimeDesc(categoryId).stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }


    // 가장 최근 체결된 목록 1개
    public TradeResponse getRecentTrade(Long categoryId) {
        return null;
    }

    // 주문 ID별 체결 상태 (체결/미체결 목록)
    public List<TradeResponse> getTradeByOrder(Long orderId) {
        return null;
    }

    // 사용자별 체결된 목록
    public List<TradeResponse> getMyTrade(Long memberId) {
        return null;
    }
}
