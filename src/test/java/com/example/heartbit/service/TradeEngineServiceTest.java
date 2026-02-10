package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderStatus;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.dto.order.OrderBookResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@ExtendWith(MockitoExtension.class)
class TradeEngineServiceTest {

    @InjectMocks
    private TradeEngineService tradeEngineService;

    @AfterEach
    void tearDown() {
        tradeEngineService.deleteAllOrders();
    }


    @DisplayName("매수 주문 시 호가창에 있는 매도 물량 중 가장 낮은 가격부터 체결된다.")
    @Test
    void processOrderWithPricePriority() {
        // given
        Category category = createCategory();

        Order expensiveSell = createOrder(1L, category, OrderType.SELL, "11000", "1");
        Order cheapSell = createOrder(2L, category, OrderType.SELL, "10000", "1");

        tradeEngineService.processOrder(expensiveSell);
        tradeEngineService.processOrder(cheapSell);

        Order buyOrder = createOrder(3L, category, OrderType.BUY, "12000", "1");

        // when
        List<TradeResponse> results = tradeEngineService.processOrder(buyOrder);

        // then
        assertThat(results).hasSize(1)
                .extracting("tradePrice", "sellOrderId")
                .containsExactly(
                        tuple(new BigDecimal("10000"), 2L)
                );
    }

    @DisplayName("매수 주문이 매도/매수 중 나중에 등록 된 주문일 때 매수/매도가 가격에 맞춰 매핑된다.")
    @Test
    void takerMakerMapping() {
        // given
        Category category = createCategory();
        Order makerSell = createOrder(10L, category, OrderType.SELL, "10000", "1");
        tradeEngineService.processOrder(makerSell);

        Order takerBuy = createOrder(20L, category, OrderType.BUY, "10000", "1");

        // when
        List<TradeResponse> results = tradeEngineService.processOrder(takerBuy);

        // then
        assertThat(results).hasSize(1);
        TradeResponse response = results.get(0);

        assertThat(response.getBuyOrderId()).isEqualTo(20L);
        assertThat(response.getSellOrderId()).isEqualTo(10L);
        assertThat(response.getTakerType()).isEqualTo("BUY");
    }

    @DisplayName("가격이 같으면 먼저 주문한 주문이 먼저 체결된다.")
    @Test
    void processOrderPriority() {
        // given
        Category category = createCategory();

        Order firstSell = createOrder(10L, category, OrderType.SELL, "50000", "1");
        Order secondSell = createOrder(11L, category, OrderType.SELL, "50000", "1");

        tradeEngineService.processOrder(firstSell);
        tradeEngineService.processOrder(secondSell);

        Order buyOrder = createOrder(12L, category, OrderType.BUY, "50000", "1");

        // when
        List<TradeResponse> results = tradeEngineService.processOrder(buyOrder);

        // then
        assertThat(results).hasSize(1)
                .extracting("sellOrderId")
                .containsExactly(10L);
    }

    @DisplayName("주문 수량이 호가창의 대기 물량보다 많으면 호가창의 수량만큼 체결되고 나머지는 호가창에 남는다.")
    @Test
    void processOrderPartial() {
        // given
        Category category = createCategory();
        Order sellOrder = createOrder(1L, category, OrderType.SELL, "50000", "1");
        tradeEngineService.processOrder(sellOrder);

        // orderId가 겹치지 않도록 설정
        Order buyOrder = createOrder(4L, category, OrderType.BUY, "50000", "3");

        // when
        List<TradeResponse> results = tradeEngineService.processOrder(buyOrder);

        // then
        assertThat(results).hasSize(1)
                .extracting("tradeCount")
                .containsExactly(new BigDecimal("1"));

        assertThat(buyOrder.getRemainingCount()).isEqualByComparingTo("2");

        List<OrderBookResponse> buyOrderBook = tradeEngineService.getMatchingOrder(category.getCategoryId())
                .getSnapshot(OrderType.BUY, 30);

        assertThat(buyOrderBook).hasSize(1)
                .extracting("orderPrice", "totalRemainingCount")
                .containsExactly(tuple(new BigDecimal("50000"), new BigDecimal("2")));
    }

    @DisplayName("하나의 주문이 호가창의 여러 주문과 체결될 수 있다.")
    @Test
    void multipleMatchingTest() {
        // given
        Category category = createCategory();
        tradeEngineService.processOrder(createOrder(1L, category, OrderType.SELL, "100", "1"));
        tradeEngineService.processOrder(createOrder(2L, category, OrderType.SELL, "110", "1"));

        Order bigBuy = createOrder(3L, category, OrderType.BUY, "120", "2.5");

        // when
        List<TradeResponse> results = tradeEngineService.processOrder(bigBuy);

        // then
        assertThat(results).hasSize(2);
        assertThat(bigBuy.getRemainingCount()).isEqualByComparingTo("0.5");

        assertThat(results).extracting("sellOrderId", "tradePrice")
                .containsExactlyInAnyOrder(
                        tuple(1L, new BigDecimal("100")),
                        tuple(2L, new BigDecimal("110"))
                );
    }

    @DisplayName("현재가를 기준으로 매수는 내림차순 매도는 오름차순으로 종목별 주문 리스트를 가져온다.")
    @Test
    void processOrderSort() {
        // given
        Category category = createCategory();
        Order sellOrder1 = createOrder(1L, category, OrderType.SELL, "50000", "1");
        Order sellOrder2 = createOrder(2L, category, OrderType.SELL, "60000", "0.5");
        Order sellOrder3 = createOrder(5L, category, OrderType.SELL, "30000", "0.5");
        Order buyOrder1 = createOrder(10L, category, OrderType.BUY, "20000", "1");
        Order buyOrder2 = createOrder(11L, category, OrderType.BUY, "15000", "2");
        Order recentTradePrice = createOrder(13L, category, OrderType.BUY, "25000", "2");

        List<Order> orders = List.of(
                sellOrder1, sellOrder2, sellOrder3,
                buyOrder1, buyOrder2, recentTradePrice
        );

        // when: 주문들을 엔진에 하나씩 투입
        for (Order order : orders) {
            tradeEngineService.processOrder(order);
        }

        // then
        TradeEngineService.MatchingOrder matchingOrder = tradeEngineService.getMatchingOrder(category.getCategoryId());

        List<OrderBookResponse> sellSnapshot = matchingOrder.getSnapshot(OrderType.SELL, 30);
        assertThat(sellSnapshot).extracting(OrderBookResponse::getOrderPrice)
                .containsExactly(new BigDecimal("30000"), new BigDecimal("50000"), new BigDecimal("60000"));

        List<OrderBookResponse> buySnapshot = matchingOrder.getSnapshot(OrderType.BUY, 30);
        assertThat(buySnapshot).extracting(OrderBookResponse::getOrderPrice)
                .containsExactly(new BigDecimal("25000"), new BigDecimal("20000"), new BigDecimal("15000"));
    }

    // 정규화관련 테스트
    @ParameterizedTest
    @CsvSource({
            "150.75, 150",   // 100 이상: 소수점 제거 (Scale 0)
            "55.55, 55.5",   // 10 이상: 소수점 1자리 (Scale 1)
            "5.555, 5.55",   // 10 미만: 소수점 2자리 (Scale 2)
            "100.00, 100"    // 경계값 테스트
    })
    @DisplayName("입력된 주문 가격은 내부 규칙에 따라 정규화된다.")
    void normalizePriceTest(String input, String expected) {
        // when
        BigDecimal result = tradeEngineService.normalizePrice(new BigDecimal(input));

        // then
        assertThat(result).isEqualByComparingTo(new BigDecimal(expected));
    }

    private Category createCategory() {
        return Category.builder()
                .categoryId(1L)
                .symbol("BTC")
                .build();
    }

    private Order createOrder(Long orderId, Category category, OrderType type, String price, String count) {
        return Order.builder()
                .orderId(orderId)
                .category(category)
                .orderType(type)
                .orderPrice(new BigDecimal(price))
                .orderCount(new BigDecimal(count))
                .remainingCount(new BigDecimal(count))
                .orderStatus(OrderStatus.OPEN)
                .build();
    }
}