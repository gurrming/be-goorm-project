package com.example.heartbit.config.Redis;

import com.example.heartbit.service.RedisOrderSubscriber;
import com.example.heartbit.service.RedisSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // AWS EC2 IP와 비밀번호를 명시적으로 설정하여 연결 팩토리 생성
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (!password.isEmpty()) {
            config.setPassword(password);
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // LocalDateTime 등 Java 8 날짜 타입을 JSON으로 변환하기 위한 필수 모듈
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       ObjectMapper objectMapper) {


        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                                   MessageListenerAdapter orderListenerAdapter,
                                                   MessageListenerAdapter tickerListenerAdapter,
                                                   MessageListenerAdapter tradesListenerAdapter,
                                                   MessageListenerAdapter chartsListenerAdapter,
                                                   MessageListenerAdapter orderbookListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(orderListenerAdapter, new ChannelTopic("order-sharding-channel"));
        container.addMessageListener(tickerListenerAdapter, new ChannelTopic("ws-ticker-channel"));
        container.addMessageListener(tradesListenerAdapter, new ChannelTopic("ws-trades-channel"));
        container.addMessageListener(chartsListenerAdapter, new ChannelTopic("ws-charts-channel"));
        container.addMessageListener(orderbookListenerAdapter, new ChannelTopic("ws-orderbook-channel"));
        return container;
    }

    @Bean
    public MessageListenerAdapter orderListenerAdapter(RedisOrderSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "handleMessage");
        adapter.setSerializer(RedisSerializer.json());
        return adapter;
    }

    @Bean
    public MessageListenerAdapter tickerListenerAdapter(RedisSubscriber subscriber) {
        // RedisSubscriber 클래스의 "sendTickerUpdate" 메서드를 실행하라고 지정
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "sendTickerUpdate");
        adapter.setSerializer(RedisSerializer.json());
        return adapter;
    }

    @Bean
    public MessageListenerAdapter tradesListenerAdapter(RedisSubscriber subscriber) {
        // RedisSubscriber 클래스의 "sendTradesUpdate" 메서드를 실행하라고 지정
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "sendTradesUpdate");
        adapter.setSerializer(RedisSerializer.json());
        return adapter;
    }

    @Bean
    public MessageListenerAdapter chartsListenerAdapter(RedisSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "sendChartsUpdate");
        adapter.setSerializer(RedisSerializer.json());
        return adapter;
    }

    @Bean
    public MessageListenerAdapter orderbookListenerAdapter(RedisSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "sendOrderbookUpdate");
        adapter.setSerializer(RedisSerializer.json());
        return adapter;
    }
}