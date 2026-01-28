package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.dto.CategoryDto;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

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
    @Mock private NotificationService notificationService;
    @Mock private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        // Redis Template Mocking (opsForValue() 호출 시 Mock 객체 반환)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("서버 시작(init) 시 DB 데이터를 읽어와 메모리 맵(시가, 현재가 등)을 초기화해야 한다")
    void init_ShouldLoadDataFromDB() {
        // given
        Long categoryId = 1L;
        Category category = Category.builder().categoryId(categoryId).build();
        given(categoryRepository.findAll()).willReturn(List.of(category));
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        Order buyOrder = Order.builder().orderType(OrderType.BUY).orderTime(LocalDateTime.now()).build();
        Order sellOrder = Order.builder().orderType(OrderType.SELL).orderTime(LocalDateTime.now()).build();

        // 오전 9시 이전 거래(시가용)
        Trade openTrade = Trade.builder()
                .tradePrice(new BigDecimal("1000"))
                .tradeCount(new BigDecimal("10"))
                .buyOrder(buyOrder)  // ★ 추가: 필수 필드 주입
                .sellOrder(sellOrder) // ★ 추가: 필수 필드 주입
                .build();
        given(tradeRepository.findTop1ByCategoryIdAndTradeTimeBefore(eq(categoryId), any(LocalDateTime.class)))
                .willReturn(Optional.of(openTrade));

        // 최신 거래(현재가용) - 시간 설정 필수 (TakerType 계산)

        Trade currentTrade = Trade.builder()
                .tradePrice(new BigDecimal("1200"))
                .tradeCount(new BigDecimal("5"))  // ★ 추가: Null 방지
                .buyOrder(buyOrder)
                .sellOrder(sellOrder)
                .tradeTime(LocalDateTime.now())
                .build();
        given(tradeRepository.findTop1ByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(categoryId))
                .willReturn(Optional.of(currentTrade));

        // 당일 체결 내역 (고가/저가/거래량 계산용)
        given(tradeRepository.findTradesByCategoryIdAndTradeTimeAfter(eq(categoryId), any(LocalDateTime.class)))
                .willReturn(List.of(openTrade, currentTrade));

        // when
        tradeService.init();

        // then: 메모리에 값이 잘 들어갔는지 getCategory(Getter 역할)로 확인
        CategoryDto result = tradeService.getCategory(categoryId);

        assertThat(result.getOpenPrice()).isEqualByComparingTo("1000");
        assertThat(result.getTradePrice()).isEqualByComparingTo("1200");
        assertThat(result.getDailyHigh()).isEqualByComparingTo("1200"); // 1000과 1200 중 최대
        assertThat(result.getDailyLow()).isEqualByComparingTo("1000");  // 1000과 1200 중 최소
    }

    @Test
    @DisplayName("체결 발생 시 자산 정산, DB 저장, 브로드캐스팅이 모두 수행되어야 한다")
    void processTradeResults_ShouldUpdateAssetsAndBroadcast() {
        // given
        Long categoryId = 1L;
        Long buyerId = 10L;
        Long sellerId = 20L;

        // 체결 응답 객체 생성
        TradeResponse tradeResponse = TradeResponse.builder()
                .buyOrderId(100L)
                .sellOrderId(200L)
                .tradePrice(new BigDecimal("50000"))
                .tradeCount(new BigDecimal("2"))
                .tradeTime(LocalDateTime.now())
                .takerType("BUY")
                .build();
        List<TradeResponse> tradeResults = List.of(tradeResponse);

        // 주문 조회 Mocking
        Member buyer = Member.builder().memberId(buyerId).build();
        Member seller = Member.builder().memberId(sellerId).build();
        Category category = Category.builder().categoryId(categoryId).build();

        Order buyOrder = Order.builder()
                .orderId(100L)
                .member(buyer)
                .category(category)
                .orderType(OrderType.BUY)
                .orderPrice(new BigDecimal("50000"))
                .remainingCount(new BigDecimal("10")) // ★ Null 방지: 넉넉하게 10개 설정
                .orderTime(LocalDateTime.now())
                .build();

        // [수정 포인트 2] 매도 주문에 remainingCount 추가
        Order sellOrder = Order.builder()
                .orderId(200L)
                .member(seller)
                .category(category)
                .orderType(OrderType.SELL)
                .orderPrice(new BigDecimal("50000"))
                .remainingCount(new BigDecimal("10")) // ★ Null 방지
                .orderTime(LocalDateTime.now().minusSeconds(10))
                .build();

        given(orderRepository.findById(100L)).willReturn(Optional.of(buyOrder));
        given(orderRepository.findById(200L)).willReturn(Optional.of(sellOrder));

        // save 호출 시 입력된 객체 그대로 반환
        given(tradeRepository.save(any(Trade.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        tradeService.processTradeResults(categoryId, tradeResults);

        // then
        // 1. 자산 서비스 호출 검증 (매수자: 차액 환불/정산, 매도자: 판매대금 입금)
        then(assetService).should().settleBuyTrade(eq(buyerId), any(BigDecimal.class), any(BigDecimal.class));
        then(assetService).should().settleSellTrade(eq(sellerId), any(BigDecimal.class));

        // 2. 투자 내역 업데이트 호출 검증 (매수자, 매도자 각각 호출)
        then(investService).should(times(2)).saveOrUpdateInvest(anyLong(), any(Trade.class), anyLong(), any(BigDecimal.class), any(BigDecimal.class), anyString());

        // 3. 체결 내역 저장 호출 검증
        then(tradeRepository).should().save(any(Trade.class));

        // 4. 웹소켓 전송 검증 (Ticker, Trade, Candle 등 여러 번 호출됨)
        then(messagingTemplate).should(times(4)).convertAndSend(anyString(), any(Object.class));

        // 5. 메모리 값 업데이트 확인 (현재가가 50000원으로 변경되었는지)
        assertThat(tradeService.getCurrentTrade(categoryId).getTradePrice()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("현재가 조회 시 메모리에 값이 있으면 메모리 값을, 없으면 DB 값을 반환하고 메모리에 캐싱해야 한다")
    void getCurrentTrade_MemoryFallbackToDB() {
        // given
        Long categoryId = 1L;

        // Scenario 1: 메모리에 값이 없을 때 -> DB 조회
        Order buyOrder = Order.builder().orderType(OrderType.BUY).orderTime(LocalDateTime.now()).build();
        Order sellOrder = Order.builder().orderType(OrderType.SELL).orderTime(LocalDateTime.now()).build();
        Trade dbTrade = Trade.builder()
                .tradePrice(new BigDecimal("3000"))
                .buyOrder(buyOrder)
                .sellOrder(sellOrder)
                .build();

        // 메모리는 비어있다고 가정 (InjectMocks 상태)
        given(tradeRepository.findTop1ByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(categoryId))
                .willReturn(Optional.of(dbTrade));

        // when (First Call - DB Hit)
        TradeResponse result1 = tradeService.getCurrentTrade(categoryId);

        // then
        assertThat(result1.getTradePrice()).isEqualByComparingTo("3000");

        // when (Second Call - Should be Memory Hit)
        // DB Mock핑을 제거하거나 verify로 호출 횟수 확인 가능하지만,
        // 여기서는 tradeRepository가 더 이상 호출되지 않아야 함을 검증하면 완벽함.
        TradeResponse result2 = tradeService.getCurrentTrade(categoryId);
        assertThat(result2.getTradePrice()).isEqualByComparingTo("3000");

        // DB 조회 메서드는 총 1번만 호출되어야 함 (두 번째는 메모리에서 가져왔으니까)
        then(tradeRepository).should(times(1)).findTop1ByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(categoryId);
    }

    @Test
    @DisplayName("초기 캔들 데이터 조회 시 1분봉 OHLC 계산이 정확해야 한다")
    void getInitialCandles_OHLCCalculation() {
        // given
        Long categoryId = 1L;
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

        Order buyOrder = Order.builder().orderType(OrderType.BUY).build();
        Order sellOrder = Order.builder().orderType(OrderType.SELL).build();

        // t1 (종가 - 가장 최신)
        Trade t1 = Trade.builder().tradePrice(new BigDecimal("1050")).tradeTime(now.plusSeconds(50)).buyOrder(buyOrder).sellOrder(sellOrder).build();
        ReflectionTestUtils.setField(t1, "tradeId", 3L);

        // t2 (고가)
        Trade t2 = Trade.builder().tradePrice(new BigDecimal("1100")).tradeTime(now.plusSeconds(30)).buyOrder(buyOrder).sellOrder(sellOrder).build();
        ReflectionTestUtils.setField(t2, "tradeId", 2L);

        // t3 (저가, 시가 - 가장 과거)
        Trade t3 = Trade.builder().tradePrice(new BigDecimal("900")).tradeTime(now.plusSeconds(10)).buyOrder(buyOrder).sellOrder(sellOrder).build();
        ReflectionTestUtils.setField(t3, "tradeId", 1L);

        // DB는 최신순(DESC)으로 데이터를 반환
        given(tradeRepository.findLatestTrades(eq(categoryId), any(Pageable.class)))
                .willReturn(List.of(t1, t2, t3));

        // when
        List<Map<String, Object>> candles = tradeService.getInitialCandles(categoryId, null, 100);

        // then
        assertThat(candles).hasSize(1);
        Map<String, Object> candle = candles.get(0);

        assertThat(candle.get("o")).isEqualTo("900");
        assertThat(candle.get("h")).isEqualTo("1100");
        assertThat(candle.get("l")).isEqualTo("900");
        assertThat(candle.get("c")).isEqualTo("1050");
    }

    @Test
    @DisplayName("오전 9시 장 초기화 시 시가는 전일 종가로, 거래량 등은 0으로 리셋되어야 한다")
    void refreshMarket_Reset() {
        // given
        Long categoryId = 1L;
        BigDecimal closePrice = new BigDecimal("5000");

        // 현재가 맵(currentPrices)에 데이터 강제 주입 (ReflectionTestUtils 사용)
        Map<Long, BigDecimal> currentPrices = new ConcurrentHashMap<>();
        currentPrices.put(categoryId, closePrice);
        ReflectionTestUtils.setField(tradeService, "currentPrices", currentPrices);

        // 누적 거래량 맵에도 데이터 주입 (초기화 확인용)
        Map<Long, BigDecimal> accVolumes = new ConcurrentHashMap<>();
        accVolumes.put(categoryId, new BigDecimal("1000"));
        ReflectionTestUtils.setField(tradeService, "accVolumes", accVolumes);

        // when
        tradeService.refreshMarket();

        // then
        // 1. 시가(openPrices)가 현재가(5000)로 업데이트 되었는지 확인
        // Map 필드를 직접 꺼내서 확인
        Map<Long, BigDecimal> openPrices = (Map<Long, BigDecimal>) ReflectionTestUtils.getField(tradeService, "openPrices");
        assertThat(openPrices.get(categoryId)).isEqualByComparingTo(closePrice);

        // 2. 거래량(accVolumes)이 비워졌는지 확인
        Map<Long, BigDecimal> resultAccVolumes = (Map<Long, BigDecimal>) ReflectionTestUtils.getField(tradeService, "accVolumes");
        assertThat(resultAccVolumes).isEmpty();
    }
}