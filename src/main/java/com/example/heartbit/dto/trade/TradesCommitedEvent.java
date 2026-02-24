package com.example.heartbit.dto.trade;

import java.util.List;

public record TradesCommitedEvent(Long categoryId, List<TradeResponse> tradeResults) {
}
