package com.example.LiveHost.repository;

import com.example.LiveHost.dto.BroadcastListResponse;
import com.example.LiveHost.dto.BroadcastResponse;
import com.example.LiveHost.dto.BroadcastSearch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface BroadcastRepositoryCustom {
    // 개별 탭 조회 (페이징)
    Slice<BroadcastListResponse> searchBroadcasts(Long sellerId, BroadcastSearch condition, Pageable pageable);

    // 전체 탭 조회 (섹션별 리스트)
    List<BroadcastListResponse> findRecentByStatusGroup(Long sellerId, String tabGroup, int limit);
}
