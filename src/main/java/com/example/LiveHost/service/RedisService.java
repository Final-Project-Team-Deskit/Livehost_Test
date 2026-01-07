package com.example.LiveHost.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static javax.swing.UIManager.getInt;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // =====================================================================
    // [Key 생성 헬퍼 Methods]
    // =====================================================================

    // 1. 실시간 시청자(Active UV): 현재 접속 중인 고유 사용자 Set
    public String getRealtimeViewKey(Long broadcastId) { return "broadcast:" + broadcastId + ":active_uv"; }

    // 2. 세션 카운트(Reference Counting): 사용자(UUID)별 열려있는 탭 개수 Hash
    public String getSessionCountKey(Long broadcastId) { return "broadcast:" + broadcastId + ":session_counts"; }

    // 3. 누적 시청자(Total UV): 방송 시작부터 끝까지 방문한 총 고유 사용자 Set (DB 저장용)
    public String getTotalUvKey(Long broadcastId) { return "broadcast:" + broadcastId + ":total_uv"; }

    // 4. 좋아요 유저 목록(Set): 좋아요를 누른 MemberId 저장 (토글용)
    public String getLikeUsersKey(Long broadcastId) { return "broadcast:" + broadcastId + ":like_users"; }

    // 5. 제재 건수 (단순 카운트)
    public String getSanctionKey(Long broadcastId) { return "broadcast:" + broadcastId + ":sanctions"; }

    // 6. 신고 건수
    public String getReportUsersKey(Long bId) { return "broadcast:" + bId + ":report_users"; } // 신고자 목록(Set)
    public String getReportCountKey(Long bId) { return "broadcast:" + bId + ":reports"; } // 신고 수(Counter)

    // 7. Write-Behind 버퍼 키 (입장/퇴장 로그 임시 저장소)
    public String getViewHistoryBufferKey(String type) { return "view_history:buffer:" + type; } // type: "entry" or "exit"

    // 8. Peak Stats Keys
    public String getMaxViewersKey(Long bId) { return "broadcast:" + bId + ":max_viewers"; }
    public String getMaxViewersTimeKey(Long bId) { return "broadcast:" + bId + ":max_viewers_time"; }

    // =====================================================================
    // 1. [동시성 제어] 분산 락 (Distributed Lock)
    // =====================================================================
    public Boolean acquireLock(String key, long timeoutMillis) {
        return redisTemplate.opsForValue()
                .setIfAbsent(key, "LOCKED", Duration.ofMillis(timeoutMillis));
    }

    public void releaseLock(String key) {
        redisTemplate.delete(key);
    }


    // =====================================================================
    // 2. [시청자 집계] 멀티 탭 지원 & 중복 제거 (Reference Counting)
    // =====================================================================

    // 입장 처리 (Connect)
    public void enterLiveRoom(Long broadcastId, String uuid) {
        String sessionKey = getSessionCountKey(broadcastId);
        String activeKey = getRealtimeViewKey(broadcastId);
        String totalKey = getTotalUvKey(broadcastId);

        // 1. 해당 유저의 세션 수 증가 (Hash)
        Long count = redisTemplate.opsForHash().increment(sessionKey, uuid, 1);

        // 2. 첫 번째 탭 입장이라면 -> 실시간 Set에 추가
        if (count == 1) {
            redisTemplate.opsForSet().add(activeKey, uuid);
            // [핵심] 시청자가 늘었으니, 최고 기록인지 확인하고 갱신
            updatePeakViewers(broadcastId);
        }

        // 3. 누적 방문자(Total UV)는 무조건 추가 (Set이므로 자동 중복 제거됨)
        redisTemplate.opsForSet().add(totalKey, uuid);

        // 키 만료시간 갱신 (방송 종료 후 자동 삭제를 위해 넉넉하게 설정)
        expireKey(sessionKey);
        expireKey(activeKey);
        expireKey(totalKey);
    }

    // 퇴장 처리 (Disconnect)
    public void exitLiveRoom(Long broadcastId, String uuid) {
        String sessionKey = getSessionCountKey(broadcastId);
        String activeKey = getRealtimeViewKey(broadcastId);

        // 1. 해당 유저의 세션 수 감소
        Long count = redisTemplate.opsForHash().increment(sessionKey, uuid, -1);

        // 2. 모든 탭을 닫았다면(0 이하) -> 실시간 Set에서 제거
        if (count <= 0) {
            redisTemplate.opsForHash().delete(sessionKey, uuid); // Hash에서 유저 삭제
            redisTemplate.opsForSet().remove(activeKey, uuid);   // Active Set에서 삭제
        }
    }

    // [조회] 실시간 시청자 수 (화면 표시용)
    public int getRealtimeViewerCount(Long broadcastId) {
        Long size = redisTemplate.opsForSet().size(getRealtimeViewKey(broadcastId));
        return size != null ? size.intValue() : 0;
    }

    // [조회] 누적 시청자 수 (방송 종료 후 DB 저장용)
    public int getTotalUniqueViewerCount(Long broadcastId) {
        Long size = redisTemplate.opsForSet().size(getTotalUvKey(broadcastId));
        return size != null ? size.intValue() : 0;
    }

    // =====================================================================
    // 3. [Write-Behind] 시청 기록 버퍼링 (DB 부하 분산 핵심 로직)
    // =====================================================================
    // 로그를 Redis List에 Push (O(1) 속도)
    public void bufferViewHistory(String type, Long broadcastId, String viewerId) {
        // Format: "broadcastId:viewerId:timestamp"
        String value = broadcastId + ":" + viewerId + ":" + System.currentTimeMillis();
        redisTemplate.opsForList().rightPush(getViewHistoryBufferKey(type), value);
    }

    // 스케줄러가 데이터를 꺼내갈 때 사용
    public List<String> popViewHistoryBuffer(String type, int count) {
        String key = getViewHistoryBufferKey(type);
        // 0부터 count-1까지 읽기
        List<Object> objects = redisTemplate.opsForList().range(key, 0, count - 1);
        if (objects == null || objects.isEmpty()) return List.of();

        // 읽은 만큼 삭제 (Trim)
        redisTemplate.opsForList().trim(key, count, -1);

        return objects.stream().map(Object::toString).collect(Collectors.toList());
    }


    // =====================================================================
    // 4. [좋아요] 토글 (Toggle) - Set 사용
    // =====================================================================
    public boolean toggleLike(Long broadcastId, Long memberId) {
        String key = getLikeUsersKey(broadcastId);

        // 이미 눌렀는지 확인
        Boolean isMember = redisTemplate.opsForSet().isMember(key, String.valueOf(memberId));

        if (Boolean.TRUE.equals(isMember)) {
            // 이미 있음 -> 취소 (삭제)
            redisTemplate.opsForSet().remove(key, String.valueOf(memberId));
            return false; // 좋아요 취소됨
        } else {
            // 없음 -> 추가 (좋아요)
            redisTemplate.opsForSet().add(key, String.valueOf(memberId));
            expireKey(key);
            return true; // 좋아요 추가됨
        }
    }

    // [조회] 실시간 좋아요 수
    public int getLikeCount(Long broadcastId) {
        Long size = redisTemplate.opsForSet().size(getLikeUsersKey(broadcastId));
        return size != null ? size.intValue() : 0;
    }

    // =====================================================================
    // 5. [신고] (SanctionKey -> ReportKey로 수정됨)
    // =====================================================================
    public boolean reportBroadcast(Long broadcastId, Long memberId) {
        String userKey = getReportUsersKey(broadcastId);
        String countKey = getReportCountKey(broadcastId);

        // 1. Set에 유저 ID 추가 (SADD)
        // 리턴값: 1 = 신규 추가됨 (첫 신고), 0 = 이미 존재함 (중복 신고)
        Long added = redisTemplate.opsForSet().add(userKey, String.valueOf(memberId));

        if (added != null && added == 1) {
            // 2. 첫 신고일 경우에만 카운트 증가
            redisTemplate.opsForValue().increment(countKey);
            // 키 만료 설정 (방송 종료 후 삭제되지만 안전장치)
            redisTemplate.expire(userKey, Duration.ofDays(1));
            return true; // 신고 성공
        }

        return false; // 이미 신고함
    }

    public int getReportCount(Long broadcastId) {
        return getInt(getReportCountKey(broadcastId));
    }

    // =====================================================================
    // 6. [기타] 카운터, Pub/Sub, 유틸
    // =====================================================================

    // 최고 기록 조회 메서드 (종료 시 사용)
    public int getMaxViewers(Long bId) {
        return getInt(getMaxViewersKey(bId));
    }

    public LocalDateTime getMaxViewersTime(Long bId) {
        Object val = redisTemplate.opsForValue().get(getMaxViewersTimeKey(bId));
        return val != null ? LocalDateTime.parse(val.toString()) : null;
    }

    // 단순 카운터 증가 (제재 건수 등)
    public void increment(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    public Integer getStatOrZero(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 메시지 발행 (채팅 등)
    public void publish(String channelTopic, Object message) {
        redisTemplate.convertAndSend(channelTopic, message);
    }

    // 방송 종료 후 키 정리
    public void deleteBroadcastKeys(Long broadcastId) {
        redisTemplate.delete(getRealtimeViewKey(broadcastId));
        redisTemplate.delete(getSessionCountKey(broadcastId));
        redisTemplate.delete(getTotalUvKey(broadcastId)); // DB 저장 완료 후 삭제 권장
        redisTemplate.delete(getLikeUsersKey(broadcastId));
        redisTemplate.delete(getSanctionKey(broadcastId));
        redisTemplate.delete(getReportUsersKey(broadcastId));
        redisTemplate.delete(getReportCountKey(broadcastId));
        redisTemplate.delete(getMaxViewersKey(broadcastId));
        redisTemplate.delete(getMaxViewersTimeKey(broadcastId));
    }

    private void expireKey(String key) {
        redisTemplate.expire(key, Duration.ofDays(1));
    }

    // 최고 시청자 수 갱신 로직
    private void updatePeakViewers(Long bId) {
        int current = getRealtimeViewerCount(bId);
        String maxKey = getMaxViewersKey(bId);

        // Redis에 저장된 기존 최고 기록 가져오기
        Integer max = getInt(maxKey);

        if (current > max) {
            // 신기록 달성 시 갱신
            redisTemplate.opsForValue().set(maxKey, String.valueOf(current));
            redisTemplate.opsForValue().set(getMaxViewersTimeKey(bId), LocalDateTime.now().toString());
        }
    }
}