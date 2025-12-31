package com.example.LiveHost.repository;

import com.example.LiveHost.dto.StatisticsResponse;

import java.util.List;

public interface BroadcastResultRepositoryCustom {
    // 1. 매출 차트
    List<StatisticsResponse.ChartData> getSalesChart(Long sellerId, String periodType);

    // 2. ARPU(객단가) 차트
    List<StatisticsResponse.ChartData> getArpuChart(Long sellerId, String periodType);

    // 3. 랭킹 조회 (이름 통일: getBroadcastRanking -> getRanking)
    List<StatisticsResponse.BroadcastRank> getRanking(Long sellerId, String sortField, boolean isDesc, int limit);
}
