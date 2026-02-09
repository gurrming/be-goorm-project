package com.example.heartbit.service;

import com.example.heartbit.domain.Asset;
import com.example.heartbit.domain.Member;
import com.example.heartbit.repository.AssetRepository;
import com.example.heartbit.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AssetConcurrencyServiceTest {

    @Autowired
    private AssetService assetService;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        assetRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("동시에 매수/매도 주문이 100개가 들어와도 자산 정합성이 유지되어야한다.")
    void concurrency_test() throws InterruptedException {

        //given
        Member member = memberRepository.save(new Member("ggg@naver.com", "qqqqqqqq", "hi")); // Member 엔티티 구조에 맞게 수정 필요
        Long memberId = member.getMemberId();

        BigDecimal initialCash = new BigDecimal("10000.00000000");
        assetRepository.save(new Asset(member, initialCash, initialCash));
        int threadCount = 100;


        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            final int index = i; // 람다 내에서 사용하기 위해 final 변수화
            executorService.submit(() -> {
                try {
                    if (index % 2 == 0) {
                        // 짝수번: 매수 주문 차감
                        assetService.settleBuyTrade(memberId, new BigDecimal("100"), new BigDecimal("100"));
                    } else {
                        // 홀수번: 매도 주문 가산
                        assetService.settleSellTrade(memberId, new BigDecimal("100"));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Asset asset = assetRepository.findByMember_MemberId(memberId).orElseThrow(() -> new NoSuchElementException("자산이 존재하지 않습니다."));

        //then
        assertThat(asset.getAssetCash()).isEqualByComparingTo(initialCash);

    }
    }

