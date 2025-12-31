package com.example.heartbit.service;

import com.example.heartbit.dto.TradeResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradeService {
    public List<TradeResponse> getTradeList(Long categoryId, int limit) {
        return null;
    }

    public TradeResponse getRecentTrade(Long categoryId) {
        return null;
    }

    public List<TradeResponse> getTradeByOrder(Long orderId) {
        return null;
    }

    public List<TradeResponse> getMyTrade(Long memberId) {
        return null;
    }
}
