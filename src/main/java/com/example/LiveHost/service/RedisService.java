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

    public void decrement(String key) {
        redisTemplate.opsForValue().decrement(key);
    }

    // 6. 메시지 방송 (확성기)
    // 예: redisService.publish("room:100", messageDto);
    public void publish(String channelTopic, Object message) {
        redisTemplate.convertAndSend(channelTopic, message);
    }

    // 숫자형 데이터 조회 (없으면 0 반환)
      // 조회수, 좋아요, 제재 건수 조회용
    public Integer getStatOrZero(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // --- [추가] Key 생성 규칙 통일 (실수 방지) ---
    private static final String KEY_PREFIX = "broadcast:";

    public String getViewKey(Long broadcastId) {
        return KEY_PREFIX + broadcastId + ":views";
    }

    public String getLikeKey(Long broadcastId) {
        return KEY_PREFIX + broadcastId + ":likes";
    }

    // [신규] 제재 건수 키
    public String getSanctionKey(Long broadcastId) {
        return KEY_PREFIX + broadcastId + ":sanctions";
    }
}