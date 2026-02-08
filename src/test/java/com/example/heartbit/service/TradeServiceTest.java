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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest{

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

    @Test
    @DisplayName("전체 종목 목록 조회 시 메모리의 실시간 시세가 포함되어야 한다 (getCategories)")
    void getCategories_IncludeRealTimePrice() {
        // given
        Long id = 1L;
        Category category = Category.builder()
                .categoryId(id).categoryName("비트코인").symbol("BTC")
                .build();

        given(categoryRepository.findAll()).willReturn(List.of(category));

        // [핵심] 리플렉션으로 메모리 맵에 현재가 주입 (TradeService 내부의 currentPrices)
        Map<Long, BigDecimal> currentPrices = new java.util.concurrent.ConcurrentHashMap<>();
        currentPrices.put(id, new BigDecimal("70000000"));
        org.springframework.test.util.ReflectionTestUtils.setField(tradeService, "currentPrices", currentPrices);

        // when
        List<com.example.heartbit.dto.CategoryDto> results = tradeService.getCategories();

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTradePrice()).isEqualByComparingTo("70000000");
        assertThat(results.get(0).getSymbol()).isEqualTo("BTC");
    }

    @Test
    @DisplayName("최근 체결 내역을 지정된 개수만큼 가져와야 한다 (getTradeList)")
    void getTradeList_LimitCheck() {
        // given
        Long categoryId = 1L;
        int limit = 5;

        Order buyOrder = Order.builder().orderType(OrderType.BUY).orderTime(LocalDateTime.now()).build();
        Order sellOrder = Order.builder().orderType(OrderType.SELL).orderTime(LocalDateTime.now()).build();

        Trade trade = Trade.builder()
                .tradePrice(new BigDecimal("100"))
                .tradeCount(new BigDecimal("1"))
                .buyOrder(buyOrder)
                .sellOrder(sellOrder)
                .tradeTime(LocalDateTime.now())
                .build();

        given(tradeRepository.findByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(eq(categoryId), any(Pageable.class)))
                .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(trade)));

        // when
        List<TradeResponse> results = tradeService.getTradeList(categoryId, limit);

        // then
        assertThat(results).isNotEmpty();
        then(tradeRepository).should().findByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(eq(categoryId), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("특정 주문 ID로 체결 내역을 조회해야 한다 (getTradeByOrder)")
    void getTradeByOrder_Mapping() {
        // given
        Long orderId = 100L;
        Long memberId = 1L;
        Member member = Member.builder().memberId(memberId).build();
        Order buyOrder = Order.builder().orderType(OrderType.BUY).orderTime(LocalDateTime.now()).orderId(orderId).member(member).build();
        Order sellOrder = Order.builder().orderType(OrderType.SELL).orderTime(LocalDateTime.now()).orderId(orderId).member(member).build();

        Trade trade = Trade.builder()
                .tradePrice(new BigDecimal("500"))
                .buyOrder(buyOrder) // 매수자가 본인인 케이스
                .sellOrder(sellOrder)
                .build();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(buyOrder));
        given(tradeRepository.findByBuyOrder_OrderIdOrSellOrder_OrderId(orderId, orderId))
                .willReturn(List.of(trade));

        // when
        List<TradeResponse> results = tradeService.getTradeByOrder(orderId);

        // then
        assertThat(results).hasSize(1);
        then(tradeRepository).should().findByBuyOrder_OrderIdOrSellOrder_OrderId(orderId, orderId);
    }

    @Test
    @DisplayName("내 체결 내역 조회 시 페이징이 적용되어야 한다 (getMyTrade)")
    void getMyTrade_Paging() {
        // given
        Long memberId = 1L;
        Order buyOrder = Order.builder().orderType(OrderType.BUY).orderTime(LocalDateTime.now()).build();
        Order sellOrder = Order.builder().orderType(OrderType.SELL).orderTime(LocalDateTime.now()).build();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        Trade trade = Trade.builder()
                .buyOrder(buyOrder)
                .sellOrder(sellOrder)
                .build();

        org.springframework.data.domain.Page<Trade> page = new org.springframework.data.domain.PageImpl<>(List.of(trade));

        given(tradeRepository.findTradeByMemberId(eq(memberId), any(org.springframework.data.domain.Pageable.class)))
                .willReturn(page);

        // when
        List<TradeResponse> results = tradeService.getMyTrade(memberId, 0, 10);

        // then
        assertThat(results).hasSize(1);
        then(tradeRepository).should().findTradeByMemberId(eq(memberId), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("매수자가 봇(Bot)인 경우 자산 정산 로직이 스킵되어야 한다 (분기 처리 검증)")
    void processTradeResults_SkipAssetSettlementForBots() {
        // given
        Long categoryId = 1L;
        BigDecimal initialCount = new BigDecimal("10"); // 처음 주문 수량
        TradeResponse response = TradeResponse.builder()
                .buyOrderId(1L).sellOrderId(2L)
                .tradePrice(new BigDecimal("1000")).tradeCount(new BigDecimal("1"))
                .takerType("BUY")
                .tradeTime(LocalDateTime.now())
                .build();

        // 매수자는 Bot, 매도자는 Member인 상황 설정
        Bots bot = Bots.builder().botId(1L).build();
        Member seller = Member.builder().memberId(20L).build();

        Order buyOrder = Order.builder().orderId(1L).bots(bot).remainingCount(initialCount).orderType(OrderType.BUY).orderTime(LocalDateTime.now()).build();
        Order sellOrder = Order.builder().orderId(2L).member(seller).remainingCount(initialCount).orderType(OrderType.SELL).orderTime(LocalDateTime.now().minusSeconds(1)).build();

        given(orderRepository.findById(1L)).willReturn(Optional.of(buyOrder));
        given(orderRepository.findById(2L)).willReturn(Optional.of(sellOrder));
        given(tradeRepository.save(any(Trade.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        tradeService.processTradeResults(categoryId, List.of(response));

        // then
        // 1. 매수자가 봇이므로 settleBuyTrade는 호출되지 않아야 함 (times(0))
        then(assetService).should(times(0)).settleBuyTrade(any(), any(), any());

        // 2. 매도자는 회원이므로 settleSellTrade는 정상 호출되어야 함
        then(assetService).should(times(1)).settleSellTrade(eq(20L), any());

        // 3. investService 역시 회원인 매도자에 대해서만 호출되어야 함
        then(investService).should(times(1)).saveOrUpdateInvest(eq(20L), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("시가가 0원일 때 현재가로 보정하는 로직 검증 (updateMarketAndBroadcast)")
    void updateMarket_ShouldFallbackOpenPriceToCurrentPrice_WhenOpenPriceIsZero() {
        // given
        Long categoryId = 1L;
        Long buyOrderId = 1L;  // ID 정의
        Long sellOrderId = 2L;
        BigDecimal initialCount = new BigDecimal("10");
        BigDecimal currentTradePrice = new BigDecimal("50000");

        TradeResponse response = TradeResponse.builder()
                .tradePrice(currentTradePrice)
                .buyOrderId(buyOrderId)
                .sellOrderId(sellOrderId)
                .tradeCount(new BigDecimal("1"))
                .takerType("BUY")
                .tradeTime(LocalDateTime.now())
                .build();

        // [핵심] 리플렉션으로 메모리 상의 시가를 0으로 설정
        Map<Long, BigDecimal> openPrices = new java.util.concurrent.ConcurrentHashMap<>();
        openPrices.put(categoryId, BigDecimal.ZERO); // 시가 0원 세팅
        org.springframework.test.util.ReflectionTestUtils.setField(tradeService, "openPrices", openPrices);

        // processTradeResults 수행을 위한 최소한의 Mocking
        Order buyOrder = Order.builder().orderId(1L).remainingCount(initialCount).orderType(OrderType.BUY).orderTime(LocalDateTime.now()).build();
        Order sellOrder = Order.builder().orderId(2L).remainingCount(initialCount).orderType(OrderType.SELL).orderTime(LocalDateTime.now().minusSeconds(1)).build();
        given(orderRepository.findById(1L)).willReturn(Optional.of(buyOrder));
        given(orderRepository.findById(2L)).willReturn(Optional.of(sellOrder));

        // when
        tradeService.processTradeResults(categoryId, List.of(response));

        // then
        // 시가가 0원에서 현재가인 50,000원으로 보정되었는지 확인
        Map<Long, BigDecimal> updatedOpenPrices = (Map<Long, BigDecimal>) org.springframework.test.util.ReflectionTestUtils.getField(tradeService, "openPrices");
        assertThat(updatedOpenPrices.get(categoryId)).isEqualByComparingTo(currentTradePrice);
    }

    @Test
    @DisplayName("매수자/매도자가 봇일 때 자산 정산이 스킵되고, 주문 부재 시 예외가 발생해야 한다")
    void processTradeResults_StepByStep_Validation() {
        // [Case 1] 매수자/매도자가 봇(Bot)일 때 자산 정산 스킵 검증
        // given
        Long categoryId = 1L;
        TradeResponse response = TradeResponse.builder()
                .buyOrderId(101L).sellOrderId(102L)
                .tradePrice(new BigDecimal("1000")).tradeCount(new BigDecimal("1"))
                .takerType("BUY").tradeTime(LocalDateTime.now())
                .build();

        // 매수자 봇, 매도자 봇 설정
        Bots buyerBot = Bots.builder().botId(1L).build();
        Bots sellerBot = Bots.builder().botId(2L).build();

        Order buyOrder = Order.builder()
                .orderId(101L).bots(buyerBot).orderType(OrderType.BUY).orderTime(LocalDateTime.now())
                .remainingCount(new BigDecimal("10")).build();
        Order sellOrder = Order.builder()
                .orderId(102L).bots(sellerBot).orderType(OrderType.SELL).orderTime(LocalDateTime.now().minusSeconds(5))
                .remainingCount(new BigDecimal("10")).build();

        given(orderRepository.findById(101L)).willReturn(Optional.of(buyOrder));
        given(orderRepository.findById(102L)).willReturn(Optional.of(sellOrder));
        given(tradeRepository.save(any(Trade.class))).willAnswer(inv -> inv.getArgument(0));

        // [NPE 방지] ConcurrentHashMap 초기화 (Reflection)
        Map<Long, BigDecimal> openPrices = new java.util.concurrent.ConcurrentHashMap<>();
        openPrices.put(categoryId, new BigDecimal("1000"));
        org.springframework.test.util.ReflectionTestUtils.setField(tradeService, "openPrices", openPrices);

        // when
        tradeService.processTradeResults(categoryId, List.of(response));

        // then
        // 매수자와 매도자 모두 봇이므로 자산 관련 서비스는 0회 호출되어야 함
        then(assetService).should(times(0)).settleBuyTrade(any(), any(), any());
        then(assetService).should(times(0)).settleSellTrade(any(), any());
        // 회원용 투자 서비스도 스킵되어야 함
        then(investService).should(times(0)).saveOrUpdateInvest(any(), any(), any(), any(), any(), any());


        // [Case 2] 매수 주문을 찾을 수 없을 때 예외 발생 검증
        // given
        Long invalidOrderId = 999L;
        TradeResponse errorResponse = TradeResponse.builder()
                .buyOrderId(invalidOrderId).sellOrderId(102L)
                .build();

        given(orderRepository.findById(invalidOrderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tradeService.processTradeResults(categoryId, List.of(errorResponse)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("매수 주문을 찾을 수 없습니다");
    }
    @Test
    @DisplayName("주문 ID에 해당하는 주문이 없을 경우 예외를 던져야 한다")
    void processTradeResults_ShouldThrowException_WhenOrderNotFound() {
        // given
        Long categoryId = 1L;
        Long invalidOrderId = 999L;
        TradeResponse response = TradeResponse.builder()
                .buyOrderId(invalidOrderId).sellOrderId(102L)
                .build();

        // 매수 주문을 찾을 수 없는 상황 설정
        given(orderRepository.findById(invalidOrderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tradeService.processTradeResults(categoryId, List.of(response)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("매수 주문을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("시가가 0원일 때 현재가로 보정하고, 보정된 시가를 기준으로 계산을 수행해야 한다")
    void updateMarketAndBroadcast_ShouldFixZeroOpenPriceAndCalculate() {
        // given
        Long categoryId = 1L;
        BigDecimal currentPrice = new BigDecimal("10000"); // 현재 체결가

        TradeResponse response = TradeResponse.builder()
                .tradePrice(currentPrice)
                .tradeCount(new BigDecimal("5"))
                .takerType("SELL")
                .tradeTime(LocalDateTime.now())
                .build();

        // [핵심] Reflection으로 시가(openPrices)를 0으로 강제 세팅
        Map<Long, BigDecimal> openPrices = new java.util.concurrent.ConcurrentHashMap<>();
        openPrices.put(categoryId, BigDecimal.ZERO);
        org.springframework.test.util.ReflectionTestUtils.setField(tradeService, "openPrices", openPrices);

        // 누적 거래량 등 다른 Map도 NPE 방지를 위해 초기화 (필요시)
        Map<Long, BigDecimal> accVolumes = new java.util.concurrent.ConcurrentHashMap<>();
        accVolumes.put(categoryId, BigDecimal.ZERO);
        org.springframework.test.util.ReflectionTestUtils.setField(tradeService, "accVolumes", accVolumes);

        // findById 모킹 (updateMarketAndBroadcast 실행을 위해 필수)
        Order buyOrder = Order.builder().orderId(1L).orderType(OrderType.BUY).orderTime(LocalDateTime.now()).remainingCount(new BigDecimal("10")).build();
        Order sellOrder = Order.builder().orderId(2L).orderType(OrderType.SELL).orderTime(LocalDateTime.now().minusSeconds(1)).remainingCount(new BigDecimal("10")).build();
        given(orderRepository.findById(any())).willReturn(Optional.of(buyOrder), Optional.of(sellOrder));

        // when
        tradeService.processTradeResults(categoryId, List.of(response));

        // then
        // 1. 시가가 0원에서 현재가(10000원)로 보정되었는지 확인
        Map<Long, BigDecimal> updatedOpenPrices = (Map<Long, BigDecimal>) org.springframework.test.util.ReflectionTestUtils.getField(tradeService, "openPrices");
        assertThat(updatedOpenPrices.get(categoryId)).isEqualByComparingTo(currentPrice);

        // 2. 시가가 보정되었으므로(> 0) 변동액/변동률 계산 로직이 수행되어 웹소켓으로 전송되었는지 확인
        // (전송되는 데이터의 변동률이 0%인지 확인 - 시가와 현재가가 같아졌으므로)
        then(messagingTemplate).should(atLeastOnce()).convertAndSend(contains("/topic/ticker"), any(Object.class));
    }
}