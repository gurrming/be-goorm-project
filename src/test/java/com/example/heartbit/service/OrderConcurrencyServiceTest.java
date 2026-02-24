package com.example.heartbit.service;

import com.example.heartbit.domain.Asset;
import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Member;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.engine.core.OrderBookCategory;
import com.example.heartbit.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class OrderConcurrencyServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderBookCategory orderBookCategory;

    @DisplayName("100명의 사용자가 동시에 매수/매도 주문을 넣었을 때 정합성이 유지되어야 한다.")
    @Test
    void orderAndMatchConcurrencyTest() throws InterruptedException {
        //given
        Member member = memberRepository.save(new Member("uujin@gmail.com", "00000000", "ujin"));
        Long memberId = member.getMemberId();

        Category category = categoryRepository.save(Category.builder().symbol("BTC").categoryName("비트코인").build());
        Long categoryId = category.getCategoryId();

        orderBookCategory.init(java.util.List.of(categoryId));

        BigDecimal initialCash = new BigDecimal("1000000000");
        assetRepository.save(new Asset(member, initialCash, initialCash));

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    // 짝수는 매수(BUY), 홀수는 매도(SELL) - 같은 가격으로 설정
                    OrderType type = (index % 2 == 0) ? OrderType.BUY : OrderType.SELL;

                    OrderRequest request = OrderRequest.builder()
                            .memberId(memberId)
                            .categoryId(categoryId)
                            .orderPrice(new BigDecimal("10000"))
                            .orderCount(new BigDecimal("1"))
                            .orderType(type)
                            .build();

                    orderService.createOrder(request);
                } catch (Exception e) {
                    System.err.println("주문 에러: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        Thread.sleep(3000);

        // then
        long totalOrders = orderRepository.count();
        long totalTrades = tradeRepository.count();

        System.out.println("최종 주문 수: " + totalOrders);
        System.out.println("최종 체결 수: " + totalTrades);

        assertThat(totalOrders).isEqualTo(100);
        assertThat(totalTrades).isGreaterThanOrEqualTo(50);
    }
}
