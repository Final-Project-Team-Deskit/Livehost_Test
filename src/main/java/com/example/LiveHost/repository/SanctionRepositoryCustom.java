package com.example.LiveHost.repository;

import com.example.LiveHost.dto.response.SanctionStatisticsResponse;
import java.util.List;

public interface SanctionRepositoryCustom {

    // 1. 판매자 강제 종료 차트 (Broadcast 테이블 기준)
    List<SanctionStatisticsResponse.ChartData> getSellerForceStopChart(String periodType);

    // 2. 시청자 제재 차트 (Sanction 테이블 기준)
    List<SanctionStatisticsResponse.ChartData> getViewerSanctionChart(String periodType);

    // 3. 판매자 강제 종료 랭킹 (Broadcast 테이블 기준)
    List<SanctionStatisticsResponse.SellerRank> getSellerForceStopRanking(String periodType, int limit);

    // 4. 시청자 제재 랭킹 (Sanction 테이블 기준)
    List<SanctionStatisticsResponse.ViewerRank> getViewerSanctionRanking(String periodType, int limit);

    SanctionTypeResult findLatestSanction(Long broadcastId, Long memberId);

    record SanctionTypeResult(Long sanctionId, String status) {}
}
