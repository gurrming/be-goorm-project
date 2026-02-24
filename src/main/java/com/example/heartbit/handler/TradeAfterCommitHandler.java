package com.example.heartbit.handler;

import com.example.heartbit.dto.trade.PriceChangedEvent;
import com.example.heartbit.dto.trade.TradeResponse;
import com.example.heartbit.dto.trade.TradesCommitedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.List;

import static com.example.heartbit.util.RedisKeyUtils.getTickerKey;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeAfterCommitHandler {

    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final TradeMarketBroadcaster broadcaster;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTradesCommitted(TradesCommitedEvent event) {
        Long categoryId = event.categoryId();
        List<TradeResponse> tradeResults = event.tradeResults();

        for (TradeResponse response : tradeResults) {
            try {
                String key = getTickerKey(categoryId);
                redisTemplate.opsForValue().set(key, response.getTradePrice().toPlainString(), Duration.ofSeconds(60));
            } catch (Exception e) {
                log.warn("Redis 캐시 업데이트 실패 categoryId={}", categoryId, e);
            }

            //웹소켓 데이터 전송
            broadcaster.updateMarketAndBroadcast(categoryId, response);

            //가격 변동 알림
            eventPublisher.publishEvent(new PriceChangedEvent(categoryId, response.getTradePrice()));
        }
    }
}
