package com.example.LiveHost.repository;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.dto.SanctionStatisticsResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

// [필수] QClass Import (패키지 경로 확인 필요)
import static com.example.LiveHost.entity.QBroadcast.broadcast;
import static com.example.LiveHost.entity.QSanction.sanction;
import static com.example.LiveHost.others.entity.QSeller.seller;

@RequiredArgsConstructor
public class SanctionRepositoryImpl implements SanctionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // =================================================================================
    // 1. 차트 데이터 (Chart)
    // =================================================================================

    // 1-1. 판매자 강제 종료 차트 (Broadcast 테이블: status = STOPPED)
    @Override
    public List<SanctionStatisticsResponse.ChartData> getSellerForceStopChart(String periodType) {
        StringTemplate dateExpr = getDateExpression(periodType, broadcast.endedAt); // 종료 시간 기준

        return queryFactory
                .select(Projections.constructor(SanctionStatisticsResponse.ChartData.class,
                        dateExpr,
                        broadcast.count())) // 강제 종료 건수
                .from(broadcast)
                .where(broadcast.status.eq(BroadcastStatus.STOPPED)) // [조건] 상태가 STOPPED 인 것만
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch();
    }

    // 1-2. 시청자 제재 차트 (Sanction 테이블: 모든 제재 건수)
    @Override
    public List<SanctionStatisticsResponse.ChartData> getViewerSanctionChart(String periodType) {
        StringTemplate dateExpr = getDateExpression(periodType, sanction.createdAt); // 제재 일시 기준

        return queryFactory
                .select(Projections.constructor(SanctionStatisticsResponse.ChartData.class,
                        dateExpr,
                        sanction.count())) // 제재 건수
                .from(sanction)
                // 조건: Sanction 테이블에 들어있는 것 자체가 제재이므로 전체 카운트 (혹은 특정 상태 필터링)
                // .where(sanction.status.in(SanctionType.MUTE, SanctionType.OUT)) // 필요 시 주석 해제
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch();
    }

    // =================================================================================
    // 2. 랭킹 데이터 (Ranking)
    // =================================================================================

    // 2-1. 판매자 강제 종료 순위 (Broadcast 테이블 -> Seller 조인)
    @Override
    public List<SanctionStatisticsResponse.SellerRank> getSellerForceStopRanking(int limit) {
        return queryFactory
                .select(Projections.constructor(SanctionStatisticsResponse.SellerRank.class,
                        seller.sellerId,   // [Check] Seller 엔티티의 ID 필드명
                        seller.name,  // [Check] 상호명 필드명 (brandName 혹은 storeName)
                        seller.phone,
                        broadcast.count())) // 강제 종료 횟수
                .from(broadcast)
                .join(broadcast.seller, seller)
                .where(broadcast.status.eq(BroadcastStatus.STOPPED))
                .groupBy(seller.sellerId)
                .orderBy(broadcast.count().desc())
                .limit(limit)
                .fetch();
    }

    // 2-2. 시청자 제재 순위 (Sanction 테이블 -> MemberId 기준)
    @Override
    public List<SanctionStatisticsResponse.ViewerRank> getViewerSanctionRanking(int limit) {
        // Sanction 엔티티에 'memberId' (Long)가 있으므로 이를 기준으로 집계
        return queryFactory
                .select(Projections.constructor(SanctionStatisticsResponse.ViewerRank.class,
                        sanction.memberId.stringValue(), // Long -> String 변환 (DTO가 String일 경우)
                        Expressions.asString("Member"),  // 닉네임은 별도 조인 없이 고정값 or 프론트 처리
                        sanction.count()))
                .from(sanction)
                .groupBy(sanction.memberId)
                .orderBy(sanction.count().desc())
                .limit(limit)
                .fetch();
    }

    // --- Helper Method ---
    private StringTemplate getDateExpression(String periodType, com.querydsl.core.types.dsl.DateTimePath<java.time.LocalDateTime> datePath) {
        String format = "DAILY".equalsIgnoreCase(periodType) ? "%Y-%m-%d" :
                "MONTHLY".equalsIgnoreCase(periodType) ? "%Y-%m" : "%Y";
        return Expressions.stringTemplate("DATE_FORMAT({0}, {1})", datePath, format);
    }
}