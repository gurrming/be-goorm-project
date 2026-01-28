package com.example.heartbit.service;

import com.example.heartbit.domain.Member;
import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.domain.Trade;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.OrderRepository;
import com.example.heartbit.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils; // [핵심] 리플렉션 유틸 import

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @InjectMocks
    private TradeService tradeService;

    @Mock private TradeRepository tradeRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private InvestService investService;
    @Mock private AssetService assetService;
    @Mock private OrderRepository orderRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        // Redis Template Mocking (opsForValue() 호출 시 Mock 객체 반환)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }







    @Test
    @DisplayName("초기 캔들 데이터 조회 시 1분봉 OHLC 계산이 정확해야 한다")
    void getInitialCandles_OHLCCalculation() {
        // given
        Long categoryId = 1L;
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

        // 2. [Trade] @Setter가 없으므로 ReflectionTestUtils 사용

        Order buyOrder = Order.builder()
                .orderType(OrderType.BUY) // 프로젝트에서 사용하는 OrderType Enum 확인 필요
                .build();

        Order sellOrder = Order.builder()
                .orderType(OrderType.SELL)
                .build();

        // t1 (종가)
        Trade t1 = Trade.builder()
                .tradePrice(new BigDecimal("1050"))
                .tradeTime(now.plusSeconds(50))
                .buyOrder(buyOrder)
                .sellOrder(sellOrder)
                .build();
        ReflectionTestUtils.setField(t1, "tradeId", 3L); // [핵심] 리플렉션으로 ID 설정

        // t2 (고가)
        Trade t2 = Trade.builder()
                .tradePrice(new BigDecimal("1100"))
                .tradeTime(now.plusSeconds(30))
                .buyOrder(buyOrder)
                .sellOrder(sellOrder)
                .build();
        ReflectionTestUtils.setField(t2, "tradeId", 2L);

        // t3 (저가, 시가)
        Trade t3 = Trade.builder()
                .tradePrice(new BigDecimal("900"))
                .tradeTime(now.plusSeconds(10))
                .buyOrder(buyOrder)
                .sellOrder(sellOrder)
                .build();
        ReflectionTestUtils.setField(t3, "tradeId", 1L);

        // DB는 최신순(DESC)으로 데이터를 반환한다고 가정
        when(tradeRepository.findLatestTrades(eq(categoryId), any(Pageable.class)))
                .thenReturn(List.of(t1, t2, t3));

        // when
        List<Map<String, Object>> candles = tradeService.getInitialCandles(categoryId, null, 100);

        // then
        assertThat(candles).hasSize(1);
        Map<String, Object> candle = candles.get(0);

        // OHLC 검증
        assertThat(candle.get("o")).isEqualTo("900");  // 시가 (시간상 제일 먼저인 t3)
        assertThat(candle.get("h")).isEqualTo("1100"); // 고가 (제일 비싼 t2)
        assertThat(candle.get("l")).isEqualTo("900");  // 저가 (제일 싼 t3)
        assertThat(candle.get("c")).isEqualTo("1050"); // 종가 (시간상 제일 나중인 t1)
    }

    @Test
    @DisplayName("장 초기화 시 동작 확인")
    void refreshMarket_Reset() {
        // when
        tradeService.refreshMarket();

        // then
        // 에러 없이 실행되는지 확인
    }
}