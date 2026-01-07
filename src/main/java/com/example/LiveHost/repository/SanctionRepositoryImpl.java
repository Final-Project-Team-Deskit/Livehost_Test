package com.example.LiveHost.repository;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.enums.SanctionType;
import com.example.LiveHost.dto.response.SanctionStatisticsResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

// [필수] QClass Import (패키지 경로 확인 필요)
import static com.example.LiveHost.entity.QBroadcast.broadcast;
import static com.example.LiveHost.entity.QSanction.sanction;
import static com.example.LiveHost.others.entity.QMember.member;
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
                .where(
                        broadcast.status.eq(BroadcastStatus.STOPPED), // [조건] 상태가 STOPPED 인 것만
                        getChartPeriodCondition(periodType, broadcast.endedAt) // 날짜 범위 필터링
                )
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
                .where(
                        sanction.status.in(SanctionType.MUTE, SanctionType.OUT),
                        getChartPeriodCondition(periodType, sanction.createdAt) // 날짜 범위 필터링
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch();
    }

    // =================================================================================
    // 2. 랭킹 데이터 (Ranking)
    // =================================================================================

    // 2-1. 판매자 강제 종료 순위 (Broadcast 테이블 -> Seller 조인)
    @Override
    public List<SanctionStatisticsResponse.SellerRank> getSellerForceStopRanking(String periodType, int limit) {
        return queryFactory
                .select(Projections.constructor(SanctionStatisticsResponse.SellerRank.class,
                        seller.sellerId,
                        seller.name,
                        seller.loginId,
                        broadcast.count()))
                .from(broadcast)
                .join(broadcast.seller, seller)
                .where(
                        broadcast.status.eq(BroadcastStatus.STOPPED),
                        getRankingPeriodCondition(periodType, broadcast.endedAt) // [추가] 기간 필터링
                )
                .groupBy(seller.sellerId)
                .orderBy(broadcast.count().desc())
                .limit(limit)
                .fetch();
    }

    // 2-2. 시청자 제재 순위 (Sanction 테이블 -> MemberId 기준)
    @Override
    public List<SanctionStatisticsResponse.ViewerRank> getViewerSanctionRanking(String periodType, int limit) {
        return queryFactory
                .select(Projections.constructor(SanctionStatisticsResponse.ViewerRank.class,
                        sanction.member.memberId.stringValue(),
                        member.name.coalesce("Unknown"), // 닉네임 조회
                        sanction.count()))
                .from(sanction)
                .leftJoin(sanction.member, member)
                .where(
                        getRankingPeriodCondition(periodType, sanction.createdAt) // [추가] 기간 필터링
                )
                .groupBy(sanction.member.memberId)
                .orderBy(sanction.count().desc())
                .limit(limit)
                .fetch();
    }

    // Helper Method
    // [Helper 1] 날짜 포맷 변환 (Group By용)
    private StringTemplate getDateExpression(String periodType, com.querydsl.core.types.dsl.DateTimePath<java.time.LocalDateTime> datePath) {
        String format = "DAILY".equalsIgnoreCase(periodType) ? "%Y-%m-%d" :
                "MONTHLY".equalsIgnoreCase(periodType) ? "%Y-%m" : "%Y";
        return Expressions.stringTemplate("DATE_FORMAT({0}, {1})", datePath, format);
    }

    // [Helper 2] 차트용 기간 (Trend): 최근 1달, 최근 1년, 최근 10년
    private BooleanExpression getChartPeriodCondition(String type, com.querydsl.core.types.dsl.DateTimePath<LocalDateTime> path) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        if ("DAILY".equalsIgnoreCase(type)) {
            // 일별 차트: 최근 30일 (2주)
            startDate = now.minusDays(6).with(LocalTime.MIN);
        } else if ("MONTHLY".equalsIgnoreCase(type)) {
            // 월별 차트: 최근 12개월 (1년)
            startDate = now.minusMonths(11).withDayOfMonth(1).with(LocalTime.MIN);
        } else {
            // 연도별 차트: 최근 10년
            startDate = now.minusYears(4).withDayOfYear(1).with(LocalTime.MIN);
        }
        return path.goe(startDate);
    }

    // [Helper 3] 랭킹용 기간 (Snapshot): 오늘 하루, 이번 달, 올해
    private BooleanExpression getRankingPeriodCondition(String type, com.querydsl.core.types.dsl.DateTimePath<LocalDateTime> path) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        if ("DAILY".equalsIgnoreCase(type)) {
            // 일별 랭킹: 오늘 00:00:00 ~ 현재
            startDate = now.with(LocalTime.MIN);
        } else if ("MONTHLY".equalsIgnoreCase(type)) {
            // 월별 랭킹: 이번 달 1일 00:00:00 ~ 현재
            startDate = now.withDayOfMonth(1).with(LocalTime.MIN);
        } else {
            // 연도별 랭킹: 올해 1월 1일 00:00:00 ~ 현재
            startDate = now.with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN);
        }
        return path.goe(startDate);
    }
}