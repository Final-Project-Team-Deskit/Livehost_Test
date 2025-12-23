package com.example.LiveHost.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // --- [기능 1] 데이터 저장 (칠판에 쓰기) ---
    // 예: redisService.setValues("view:100", "50", Duration.ofMinutes(1));
    public void setValues(String key, String data, Duration duration) {
        redisTemplate.opsForValue().set(key, data, duration);
    }

    // 저장 (만료시간 없이 영구 저장)
    public void setValues(String key, String data) {
        redisTemplate.opsForValue().set(key, data);
    }

    // --- [기능 2] 데이터 조회 (칠판 읽기) ---
    // 예: String views = redisService.getValues("view:100");
    public String getValues(String key) {
        Object o = redisTemplate.opsForValue().get(key);
        return o != null ? o.toString() : null;
    }

    // --- [기능 3] 데이터 삭제 (칠판 지우기) ---
    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }

    // --- [기능 4] 숫자 증가/감소 (조회수 카운팅용) ---
    // 0.001초 만에 +1 해줌
    public void increment(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    // --- [기능 5] 메시지 방송 (확성기) ---
    // 예: redisService.publish("room:100", messageDto);
    public void publish(String channelTopic, Object message) {
        redisTemplate.convertAndSend(channelTopic, message);
    }
}