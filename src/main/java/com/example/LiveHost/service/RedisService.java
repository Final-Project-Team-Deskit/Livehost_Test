package com.example.LiveHost.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 1. 데이터 저장
    // 예: redisService.setValues("view:100", "50", Duration.ofMinutes(1));
    public void setValues(String key, String data, Duration duration) {
        redisTemplate.opsForValue().set(key, data, duration);
    }

    // 2. 저장 (만료시간 없이 영구 저장)
    public void setValues(String key, String data) {
        redisTemplate.opsForValue().set(key, data);
    }

    // 3. 데이터 조회
    // 예: String views = redisService.getValues("view:100");
    public String getValues(String key) {
        Object o = redisTemplate.opsForValue().get(key);
        return o != null ? o.toString() : null;
    }

    // 4. 데이터 삭제
    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }

    // 5. 숫자 증가/감소 (조회수 카운팅용)
    public void increment(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    // 6. 메시지 방송 (확성기)
    // 예: redisService.publish("room:100", messageDto);
    public void publish(String channelTopic, Object message) {
        redisTemplate.convertAndSend(channelTopic, message);
    }
}