package com.example.LiveHost.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration // 스프링 설정 파일
public class RedisConfig {

    // 1. RedisTemplate: Redis 명령 도구(리모컨)
    // Spring이 Redis 명령을 편하게 내릴 수 있게 도와주는 도구
    @Bean // 스프링에 빈으로 등록
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // RedisTemplate 객체 초기화
            // opsForValue() -> 문자열/슷자/객체 저장 가능
            // opsForHash() -> Hash(Map) 저장 가능
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 2. 연결 공장 설정: Redis 서버와의 연결 설정
        // application.yml에 적힌 host, port 정보를 바탕으로 연결
        template.setConnectionFactory(connectionFactory);

        // 3. Key 직렬화 설정 -> 사람이 읽을 수 있게 저장
        // 설정 안 함: Redis에 "\xac\xed\x00\x05t\x00\x03key" 같은 이상한 기계어로 저장됨.
        // 설정 함: 우리가 아는 글자 그대로 "broadcast:100" 처럼 저장됨.
        template.setKeySerializer(new StringRedisSerializer());

        // 4. Value 직렬화 설정
        // 값을 저장할 때도 자바 객체를 사람이 읽을 수 있는 문자열(JSON 등)로 바꿔서 저장하겠다는 뜻.
        template.setValueSerializer(new StringRedisSerializer());

        // 5. Hash 자료구조 직렬화(HSET, HGET)
        // Redis의 'Hash' 타입(Map과 비슷)을 쓸 때도 Key/Value를 문자로 변환.
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        return template;
    }

    // 6. RedisMessageListenerContainer (듣는 역할 - Subscriber)
    // 이게 있어야 Pub/Sub 가능
    // Redis 채널로부터 메시지가 오면, 해당 리스너(Service)에게 전달해주는 컨테이너
    // 별도 설정이나 서비스에서 container.addMessageListener() 같은 걸로 구독 채널을 붙임
    @Bean
    public RedisMessageListenerContainer redisMessageListener(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
