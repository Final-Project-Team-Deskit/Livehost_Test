package com.example.LiveHost.repository;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.dto.BroadcastListResponse;
import com.example.LiveHost.dto.BroadcastResponse;
import com.example.LiveHost.dto.BroadcastSearch;
import com.querydsl.core.types.OrderSpecifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;

public interface BroadcastRepositoryCustom {
    // 1. 상세 목록 조회 (페이징 + 필터 + isAdmin 권한)
    Slice<BroadcastListResponse> searchBroadcasts(Long sellerId, BroadcastSearch condition, Pageable pageable, boolean isAdmin);

    // 2. Overview용 Top 5 조회 (isAdmin 권한 추가)
    List<BroadcastListResponse> findTop5ByStatus(Long sellerId, List<BroadcastStatus> statuses, OrderSpecifier<?> orderSpecifier, boolean isAdmin);

    // 3. 예약 슬롯 확인, 단순히 시작 시간을 기준으로 하면 되지만, 추후에 확장성을 고려해서 동일 시간대에 방송 개수를 확인하는 것으로 변경
    long countByTimeSlot(LocalDateTime start, LocalDateTime end);
}
