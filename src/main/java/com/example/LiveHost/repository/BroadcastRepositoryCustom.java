package com.example.LiveHost.repository;

import com.example.LiveHost.dto.BroadcastResponse;
import com.example.LiveHost.dto.BroadcastSearch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface BroadcastRepositoryCustom {
    // Page 대신 Slice 반환
    Slice<BroadcastResponse> searchBroadcasts(Long sellerId, BroadcastSearch condition, Pageable pageable);
}
