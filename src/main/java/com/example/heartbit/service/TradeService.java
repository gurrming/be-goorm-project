package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.dto.CategoryDto;
import com.example.heartbit.dto.trade.*;
import com.example.heartbit.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.TradeRepository;


import static com.example.heartbit.util.RedisKeyUtils.getTickerKey;


@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final CategoryRepository categoryRepository;
    private final TradeJdbcRepository tradeJdbcRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final InvestService investService;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;
    private final AssetService assetService;

    private final StringRedisTemplate redisTemplate;

    private final NotificationService notificationService;

    // 종목별 실시간 시세 상태 관리 (메모리 맵)
    private final Map<Long, BigDecimal> openPrices = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> currentPrices = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> changeAmounts = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> changeRates = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> dailyHighs = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> dailyLows = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> accVolumes = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> accAmounts = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> totalBuyQtys = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> totalSellQtys = new ConcurrentHashMap<>();
    private final Map<Long, String> takerType = new ConcurrentHashMap<>();

    // 종목별 차트용 변수 (메모리 맵)
    private final Map<Long, BigDecimal> candleOpens = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> candleHighs = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> candleLows = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> currentMinutes = new ConcurrentHashMap<>();

    /**
     * 서버 재시작 시 오늘 오전 9시 이후의 시세 데이터를 DB에서 복구
     */
    @Transactional(readOnly = true)
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today9AM = now.withHour(9).withMinute(0).withSecond(0).withNano(0);
        if (now.isBefore(today9AM)) today9AM = today9AM.minusDays(1);

        List<Category> categories = categoryRepository.findAll();

        for (Category category : categories) {
            Long id = category.getCategoryId();

            // 기준가 및 현재가 로드(오전 9시 이전의 체결이 있다면)
            tradeRepository.findTop1ByCategoryIdAndTradeTimeBeforeOrderByTradeTimeDesc(id, today9AM)
                    .ifPresent(t -> {
                        openPrices.put(id, t.getTradePrice());
                    });

            //종목별로 최근 체결 내역 1건 가져옴
            tradeRepository.findTop1ByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(id)
                    .ifPresent(t -> {
                        BigDecimal price = t.getTradePrice();
                        currentPrices.put(id, price);

                        String type = t.getBuyOrder().getOrderTime().isAfter(t.getSellOrder().getOrderTime()) ? "BUY" : "SELL";
                        takerType.put(id, type);
                        candleOpens.put(id, price);
                        candleHighs.put(id, price);
                        candleLows.put(id, price);
                        currentMinutes.put(id, t.getTradeTime().withSecond(0).withNano(0));
                    });


            // 여기서는 리스트를 쓰면 최소한 거래대금과 체결강도용 수량도 복구
            List<Trade> todayTrades = tradeRepository.findTradesByCategoryIdAndTradeTimeAfter(id, today9AM);
            if (!todayTrades.isEmpty()) {
                dailyHighs.put(id, todayTrades.stream().map(Trade::getTradePrice).max(BigDecimal::compareTo).get());
                dailyLows.put(id, todayTrades.stream().map(Trade::getTradePrice).min(BigDecimal::compareTo).get());

                // 거래량 및 거래대금 복구
                accVolumes.put(id, todayTrades.stream().map(Trade::getTradeCount).reduce(BigDecimal.ZERO, BigDecimal::add));
                accAmounts.put(id, todayTrades.stream().map(t -> t.getTradePrice().multiply(t.getTradeCount())).reduce(BigDecimal.ZERO, BigDecimal::add));


                //총 매수 거래량
                BigDecimal buyVol = todayTrades.stream()
                        .filter(t -> t.getBuyOrder().getOrderTime().isAfter(t.getSellOrder().getOrderTime())) // 매수자가 늦게 주문했으면 매수 체결(BUY Taker)
                        .map(Trade::getTradeCount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                //총 매도 거래량
                BigDecimal sellVol = todayTrades.stream()
                        .filter(t -> t.getSellOrder().getOrderTime().isAfter(t.getBuyOrder().getOrderTime())) // 매도자가 늦게 주문했으면 매도 체결(SELL Taker)
                        .map(Trade::getTradeCount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                totalBuyQtys.put(id, buyVol);
                totalSellQtys.put(id, sellVol);

                BigDecimal currentPrice = currentPrices.getOrDefault(id, BigDecimal.ZERO);
                BigDecimal openPrice = openPrices.getOrDefault(id, BigDecimal.ZERO);

                // 만약 오전 9시 이전 데이터가 없어서 시가가 0인데, 현재가는 있는 경우 (신규 상장 or 데이터 유실 등)
                // 시가를 현재가로 맞춰주어 변동률을 0%로 시작하게 함 (방어 로직)
                if (openPrice.compareTo(BigDecimal.ZERO) == 0) {
                    openPrice = todayTrades.get(0).getTradePrice();
                    openPrices.put(id, openPrice);
                }

                // 시가가 존재할 때만 계산 수행
                if (openPrice.compareTo(BigDecimal.ZERO) > 0) {
                    // 변동금 = 현재가 - 시가
                    BigDecimal changeAmount = currentPrice.subtract(openPrice);

                    // 변동률 = (변동금 / 시가) * 100
                    BigDecimal changeRate = changeAmount.divide(openPrice, 10, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));

                    // **여기서 맵에 넣어줘야 REST API 호출 시 0이 안 나옴**
                    changeAmounts.put(id, changeAmount);
                    changeRates.put(id, changeRate);
                }


            }
        }
    }

    /**
     * 체결 엔진의 체결 결과 리스트를 순회하며 DB 저장 및 시세를 업데이트
     */
    @Transactional
    @Operation(summary = "체결 엔진 결과 처리", description = "체결 데이터를 저장하고 매도자에게 대금을 정산합니다.")
    public void processTradeResults(Long categoryId, List<TradeResponse> tradeResults) {
        if (tradeResults.isEmpty()) return;

        List<Trade> tradesToSave = new ArrayList<>();

        Map<Long, BigDecimal> executionAmounts = new HashMap<>();
        Map<Long, BigDecimal> executionCounts = new HashMap<>();
        Map<Long, BigDecimal> buyBlockedAmounts = new HashMap<>();
        Map<Long, Member> memberMap = new HashMap<>();
        Map<Long, Order> lastOrderMap = new HashMap<>();

        for (TradeResponse response : tradeResults) {
            Order buyOrder = orderRepository.findById(response.getBuyOrderId())
                    .orElseThrow(() -> new NoSuchElementException("매수 주문을 찾을 수 없습니다."));

            Order sellOrder = orderRepository.findById(response.getSellOrderId())
                    .orElseThrow(() -> new NoSuchElementException("매도 주문을 찾을 수 없습니다."));
            // 주문 수량 변경 값 db 저장

            buyOrder.updateRemainingCount(response.getTradeCount()); // Order 엔티티에 해당 메서드가 있다고 가정
            sellOrder.updateRemainingCount(response.getTradeCount());


            BigDecimal tradeAmount = response.getTradePrice().multiply(response.getTradeCount());
            String takerType = buyOrder.getOrderTime().isAfter(sellOrder.getOrderTime()) ? "BUY" : "SELL";

            Trade trade = Trade.builder()
                    .tradePrice(response.getTradePrice())
                    .tradeCount(response.getTradeCount())
                    .tradeClosePrice(response.getTradeClosePrice())
                    .buyOrder(buyOrder)
                    .sellOrder(sellOrder)
                    .tradeTime(response.getTradeTime())
                    .takerType(takerType)
                    .build();

            tradesToSave.add(trade);

            if (buyOrder.getMember() != null) {
                Long memberId = buyOrder.getMember().getMemberId();
                executionAmounts.merge(memberId, tradeAmount, BigDecimal::add);
                executionCounts.merge(memberId, response.getTradeCount(), BigDecimal::add);
                buyBlockedAmounts.merge(memberId, buyOrder.getOrderPrice().multiply(response.getTradeCount()), BigDecimal::add);
                memberMap.put(memberId, buyOrder.getMember());
                lastOrderMap.put(memberId, buyOrder);
            }

            // 매도자 Asset 정보 수집
            if (sellOrder.getMember() != null) {
                Long memberId = sellOrder.getMember().getMemberId();
                executionAmounts.merge(memberId, tradeAmount, BigDecimal::add);
                executionCounts.merge(memberId, response.getTradeCount(), BigDecimal::add);
                memberMap.put(memberId, sellOrder.getMember());
                lastOrderMap.put(memberId, sellOrder);
            }
        }

        tradeJdbcRepository.bulkInsertWithKeys(tradesToSave);


        List<TradesCompletedEvent.TradeDetail> eventDetails = new ArrayList<>();
        for (Trade trade : tradesToSave) {
            // Member가 null인지 먼저 확인하고, null이면 memberId도 null로 넘깁니다.
            Long buyerId = trade.getBuyOrder().getMember() != null ? trade.getBuyOrder().getMember().getMemberId() : null;
            Long sellerId = trade.getSellOrder().getMember() != null ? trade.getSellOrder().getMember().getMemberId() : null;

            eventDetails.add(new TradesCompletedEvent.TradeDetail(
                    buyerId,
                    sellerId,
                    trade.getTradePrice(),
                    trade.getTradeCount(),
                    trade.getTradeId()
            ));
        }



            //종목별 상태 업데이트 및 웹소켓 전송

            for (Long memberId : memberMap.keySet()) {
                Member member = memberMap.get(memberId);
                Order lastOrder = lastOrderMap.get(memberId);
                BigDecimal totalAmount = executionAmounts.get(memberId);
                BigDecimal totalCount = executionCounts.get(memberId);

                if (lastOrder.getOrderType() == OrderType.BUY) {
                    assetService.settleBuyTrade(memberId, totalAmount, buyBlockedAmounts.get(memberId));
                } else {
                    assetService.settleSellTrade(memberId, totalAmount);
                }
            }

        BigDecimal referencePrice = openPrices.getOrDefault(categoryId, tradeResults.get(0).getTradePrice());

        // 알림용

        eventPublisher.publishEvent(new TradesCompletedEvent(categoryId, eventDetails));

        eventPublisher.publishEvent(new TradeNotificationEvent(categoryId, tradeResults, referencePrice));

        eventPublisher.publishEvent(new TradesCommitedEvent(categoryId, tradeResults));

    }





    //최근 체결 기록(limit으로 개수 설정 가능)
    @Transactional(readOnly = true)
    public List<TradeResponse> getTradeList(Long categoryId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return tradeRepository.findByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(categoryId, pageable)
                .stream()
                .map(TradeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    //해당 종목의 현재가 1개 가져오기
    @Transactional(readOnly = true)
    public TradeResponse getRecentTrade(Long categoryId) {
        return tradeRepository.findTop1ByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(categoryId)
                .map(TradeResponse::fromEntity)
                .orElse(null);
    }


    //주문별로 실제로 얼마나 체결되었는지 확인하기
    public List<TradeResponse> getTradeByOrder(Long orderId) {
        // 먼저 주문 정보를 가져와서 작성자 누군지 확인
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다."));
        Long memberId = order.getMember().getMemberId();

        // 해당 주문과 연결된 체결 내역을 가져온 뒤, 사용자 ID를 넘겨서 타입을 결정
        return tradeRepository.findByBuyOrder_OrderIdOrSellOrder_OrderId(orderId, orderId).stream()
                .map(t -> TradeResponse.fromEntityWithOrderType(t, memberId)) // 사용자 ID 전달
                .collect(Collectors.toList());
    }

    //개인 체결 내역
    @Transactional(readOnly = true)
    public List<TradeResponse> getMyTrade(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Trade> rawTrades = tradeRepository.findTradeByMemberId(memberId, pageable).getContent();

        // [변경 포인트 1] Key를 Long(주문번호)에서 String(주문번호_체결가격)으로 변경합니다.
        Map<String, List<Trade>> groupedByOrderAndPrice = rawTrades.stream()
                .collect(Collectors.groupingBy(t -> {
                    boolean iAmBuyer = t.getBuyOrder().getMember() != null
                            && t.getBuyOrder().getMember().getMemberId().equals(memberId);

                    Long myOrderId = iAmBuyer ? t.getBuyOrder().getOrderId() : t.getSellOrder().getOrderId();

                    // 주문 번호와 체결 가격을 합쳐서 고유 키를 만듭니다 (예: "15_11.1")
                    return myOrderId + "_" + t.getTradePrice().stripTrailingZeros().toPlainString();
                }, LinkedHashMap::new, Collectors.toList()));

        // [변경 포인트 2] values()를 순회하여 응답 객체 생성
        return groupedByOrderAndPrice.values().stream()
                .map(trades -> {
                    Trade firstTrade = trades.get(0);

                    boolean iAmBuyer = firstTrade.getBuyOrder().getMember() != null
                            && firstTrade.getBuyOrder().getMember().getMemberId().equals(memberId);

                    OrderType mySide = iAmBuyer ? OrderType.BUY : OrderType.SELL;
                    Order myOrder = iAmBuyer ? firstTrade.getBuyOrder() : firstTrade.getSellOrder();

                    String symbol = myOrder.getCategory().getSymbol();

                    // 묶인 데이터들의 수량을 모두 더해줍니다.
                    BigDecimal totalCount = trades.stream()
                            .map(Trade::getTradeCount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return TradeResponse.builder()
                            .tradeId(firstTrade.getTradeId())
                            .categoryId(myOrder.getCategory().getCategoryId())
                            .symbol(symbol)
                            .tradePrice(firstTrade.getTradePrice())
                            .tradeCount(totalCount) // 합산된 수량 (예: 16개 + 15개 ... = 300개)
                            .tradeTime(firstTrade.getTradeTime())
                            .myOrderType(mySide)
                            .build();
                })
                .toList();
    }
    @Transactional(readOnly = true)
    public TradeResponse getVolumePower(Long categoryId) {
        // 1. 기준 시간 설정 (현재로부터 24시간 전)
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);

        // 2. 매수/매도 거래량 조회 (DB 쿼리 호출)
        // takerType이 "BUY"면 매수세, "SELL"이면 매도세로 판단
        BigDecimal buyVolume = tradeRepository.sumVolume24h(categoryId, startTime, "BUY");
        BigDecimal sellVolume = tradeRepository.sumVolume24h(categoryId, startTime, "SELL");

        // 3. 체결강도 계산 공식: (매수총량 / 매도총량) * 100
        BigDecimal strength;

        if (sellVolume.compareTo(BigDecimal.ZERO) == 0) {
            // 매도량이 0인 경우 (나누기 0 방지)
            if (buyVolume.compareTo(BigDecimal.ZERO) > 0) {
                strength = new BigDecimal("100.00"); // 매수만 있으면 100%
            } else {
                strength = BigDecimal.ZERO; // 거래 아예 없음
            }
        } else {
            // 소수점 2자리까지 반올림 계산
            strength = buyVolume.divide(sellVolume, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return TradeResponse.builder()
                .categoryId(categoryId)
                .intensity(strength)
                .totalBuyVolume(buyVolume)
                .totalSellVolume(sellVolume)
                .build();
    }


    //사용자가 처음에 접속했을때 텅빈 화면이 뜨는것을 방지하기 위해 db에서 지난 차트 데이터들을 REST API로 불러오기
    public List<Map<String, Object>> getInitialCandles(Long categoryId, Long lastId, int size) {

        Pageable pageable = PageRequest.of(0, size);
        List<Trade> trades;

        if(lastId == null || lastId == 0) {
            trades = tradeRepository.findLatestTrades(categoryId, pageable);
        } else {
            trades = tradeRepository.findTradesByCursor(categoryId, lastId, pageable);
        }

        if (trades.isEmpty()) return Collections.emptyList();

        //1분봉으로 그룹핑
        Map<LocalDateTime, List<Trade>> groupedTrades = trades.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTradeTime().withSecond(0).withNano(0),
                        () -> new TreeMap<LocalDateTime, List<Trade>>(Comparator.reverseOrder()),
                        Collectors.toList()
                ));

        return groupedTrades.entrySet().stream().map(entry -> {
            LocalDateTime minute = entry.getKey();
            List<Trade> minuteTrades = entry.getValue();

            // DB에서 Desc로 가져왔기 때문에 리스트의 끝[size-1]이 그 분의 첫 거래
            BigDecimal open = minuteTrades.get(minuteTrades.size() - 1).getTradePrice(); // 시가
            BigDecimal close = minuteTrades.get(0).getTradePrice(); // 종가

            BigDecimal high = minuteTrades.stream()
                    .map(Trade::getTradePrice)
                    .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal low = minuteTrades.stream()
                    .map(Trade::getTradePrice)
                    .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            Long minTradeIdInCandle = minuteTrades.stream()
                    .map(Trade::getTradeId)
                    .min(Long::compareTo).orElse(0L);

            Map<String, Object> candle = new HashMap<>();
            candle.put("t", minute.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            candle.put("o", open.toPlainString());
            candle.put("h", high.toPlainString());
            candle.put("l", low.toPlainString());
            candle.put("c", close.toPlainString());
            candle.put("tradeId", minTradeIdInCandle);

            return candle;
        }).collect(Collectors.toList());
    }



    //오전 9시되면 장 초기화
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void refreshMarket() {
        for (Long id : currentPrices.keySet()) {
            BigDecimal closePrice = currentPrices.get(id);
            openPrices.put(id, closePrice);

            // 고가/저가를 현재가로 초기화 (0으로 하면 비교 로직이 꼬임)
            dailyHighs.put(id, closePrice);
            dailyLows.put(id, closePrice);
        }

        // 누적 데이터 전량 삭제
        accVolumes.clear();
        accAmounts.clear();
        totalBuyQtys.clear();
        totalSellQtys.clear();
    }

    /**
     * 특정 종목(categoryId)의 실시간 현재가 정보를 반환
     */
    public TradeResponse getCurrentTrade(Long categoryId) {
        String key = getTickerKey(categoryId);

        String cachedPrice = redisTemplate.opsForValue().get(key);

        if (cachedPrice != null) {
            return TradeResponse.builder()
                    .tradePrice(new BigDecimal(cachedPrice))
                    .build();
        }
        TradeResponse recent = getRecentTrade(categoryId);
        BigDecimal price = (recent != null) ? recent.getTradePrice() : BigDecimal.ZERO;

        if (price.compareTo(BigDecimal.ZERO) > 0) {
            redisTemplate.opsForValue()
                    .set(key, price.toPlainString(), Duration.ofSeconds(60));
        }


        return TradeResponse.builder()
                .tradePrice(price)
                .build();
    }
    /**
     * 종목 단건 조회 (투자용)
     */
    public CategoryDto getCategory(Long categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 종목입니다."));

        BigDecimal openPrice = openPrices.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal currentPrice = currentPrices.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal changeAmount = changeAmounts.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal changeRate = changeRates.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal dailyHigh = dailyHighs.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal dailyLow = dailyLows.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal accVolume = accVolumes.getOrDefault(categoryId, BigDecimal.ZERO);
        BigDecimal accAmount = accAmounts.getOrDefault(categoryId, BigDecimal.ZERO);
        String type = "";
        Trade lastTrade = tradeRepository.findTop1ByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(categoryId)
                .orElse(null);

        if (lastTrade != null && lastTrade.getTakerType() != null) {
            type = lastTrade.getTakerType();
        }

        BigDecimal changeAmountHigh = dailyHigh.subtract(openPrice);
        BigDecimal changeAmountLow = dailyLow.subtract(openPrice);

        BigDecimal changeRateHigh = openPrice.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                changeAmountHigh.divide(openPrice, 10, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        BigDecimal changeRateLow = openPrice.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                changeAmountLow.divide(openPrice, 10, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));


        return CategoryDto.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .symbol(category.getSymbol())
                .tradePrice(currentPrice)
                .takerType(type)
                .openPrice(openPrice)
                .changeAmount(changeAmount)
                .changeRate(changeRate.setScale(2, RoundingMode.HALF_UP)) // 소수점 2자리 포맷팅
                .dailyHigh(dailyHigh)
                .dailyLow(dailyLow)
                .changeRateHigh(changeRateHigh.setScale(2, RoundingMode.HALF_UP))
                .changeRateLow(changeRateLow.setScale(2, RoundingMode.HALF_UP))
                .accVolume(accVolume)
                .accAmount(accAmount)
                .build();
    }

    public List<CategoryDto> getCategories() {
        return categoryRepository.findAll()
                .stream()
                // 삭제되지 않은 종목만 필터링
                .map(category -> {
                    Long id = category.getCategoryId();

                    BigDecimal openPrice = openPrices.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal price = currentPrices.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal changeAmount = changeAmounts.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal changeRate = changeRates.getOrDefault(id, BigDecimal.ZERO);

                    BigDecimal dailyHigh = dailyHighs.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal dailyLow = dailyLows.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal accVolume = accVolumes.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal accAmount = accAmounts.getOrDefault(id, BigDecimal.ZERO);
                    String type = takerType.getOrDefault(id, "");


                    return CategoryDto.builder()
                            .categoryId(id)
                            .categoryName(category.getCategoryName())
                            .symbol(category.getSymbol())
                            .tradePrice(price)
                            .openPrice(openPrice)
                            .takerType(type)
                            .changeAmount(changeAmount)
                            .changeRate(changeRate.setScale(2, RoundingMode.HALF_UP))
                            .dailyHigh(dailyHigh)
                            .dailyLow(dailyLow)
                            .accVolume(accVolume)
                            .accAmount(accAmount)
                            .build();
                })
                .toList();
    }


}