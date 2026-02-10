package com.example.heartbit.disruptor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final OrderEventProducer orderEventProducer;

    /**
     * 주문 생성 완료 이벤트 핸들러
     * * @TransactionalEventListener 트랜잭션의 특정 상태에 맞춰 동작한다.
     * * phase = TransactionPhase.AFTER_COMMIT:
     * DB 트랜잭션이 최종적으로 커밋 된 직후에 이 로직을 실행합니다.
     * 만약 주문 과정에서 에러가 발생해 롤백 된다면 이 이벤트는 발행되지 않는다.
     * 이를 통해 데이터 불일치를 방지한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        orderEventProducer.publish(event.getOrder());
    }
}
