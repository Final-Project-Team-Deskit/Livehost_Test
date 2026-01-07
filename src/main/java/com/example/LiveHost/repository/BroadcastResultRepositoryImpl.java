package com.example.LiveHost.repository;

import com.example.LiveHost.dto.response.StatisticsResponse;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static com.example.LiveHost.entity.QBroadcast.broadcast;
import static com.example.LiveHost.entity.QBroadcastResult.broadcastResult;

@RequiredArgsConstructor
public class BroadcastResultRepositoryImpl implements BroadcastResultRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // 1. 기간별 매출 차트 조회
    @Override
    public List<StatisticsResponse.ChartData> getSalesChart(Long sellerId, String periodType) {
        StringTemplate dateExpr = getDateExpression(periodType);

        return queryFactory
                .select(Projections.constructor(StatisticsResponse.ChartData.class,
                        dateExpr,                       // X축
                        broadcastResult.totalSales.sum() // Y축
                ))
                .from(broadcastResult)
                .join(broadcastResult.broadcast, broadcast)
                .where(
                        sellerIdEq(sellerId),
                        broadcast.endedAt.isNotNull(),
                        getChartPeriodCondition(periodType, broadcast.startedAt)
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch();
    }

    // 2. 기간별 ARPU(객단가) 차트 조회
    @Override
    public List<StatisticsResponse.ChartData> getArpuChart(Long sellerId, String periodType) {
        StringTemplate dateExpr = getDateExpression(periodType);

        // ARPU = SUM(Sales) / SUM(Views)
        NumberExpression<BigDecimal> arpu = broadcastResult.totalSales.sum()
                .divide(broadcastResult.totalViews.sum().nullif(0));

        return queryFactory
                .select(Projections.constructor(StatisticsResponse.ChartData.class,
                        dateExpr,
                        arpu
                ))
                .from(broadcastResult)
                .join(broadcastResult.broadcast, broadcast)
                .where(
                        sellerIdEq(sellerId),
                        broadcast.endedAt.isNotNull(),
                        getChartPeriodCondition(periodType, broadcast.startedAt)
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch();
    }

    // 3. 방송 랭킹 조회 (메서드명 getRanking으로 통일)
    @Override
    public List<StatisticsResponse.BroadcastRank> getRanking(Long sellerId, String periodType, String sortField, boolean isDesc, int limit) {
        return queryFactory
                .select(Projections.constructor(StatisticsResponse.BroadcastRank.class,
                        broadcast.broadcastId,
                        broadcast.broadcastTitle,
                        broadcastResult.totalSales,
                        broadcastResult.totalViews
                ))
                .from(broadcastResult)
                .join(broadcastResult.broadcast, broadcast)
                .where(
                        sellerIdEq(sellerId),
                        broadcast.endedAt.isNotNull(),
                        getRankingPeriodCondition(periodType, broadcast.startedAt) // [핵심] "이번 달 랭킹", "올해 랭킹" 등을 위해 필요
                )
                .orderBy(getOrderSpecifier(sortField, isDesc))
                .limit(limit)
                .fetch();
    }

    // --- Helper Methods ---
    private BooleanExpression sellerIdEq(Long sellerId) {
        return sellerId != null ? broadcast.seller.sellerId.eq(sellerId) : null;
    }

    private StringTemplate getDateExpression(String periodType) {
        String format = "DAILY".equalsIgnoreCase(periodType) ? "%Y-%m-%d" :
                "MONTHLY".equalsIgnoreCase(periodType) ? "%Y-%m" : "%Y";
        return Expressions.stringTemplate("DATE_FORMAT({0}, {1})", broadcast.startedAt, format);
    }

    private OrderSpecifier<?> getOrderSpecifier(String sortField, boolean isDesc) {
        if ("SALES".equalsIgnoreCase(sortField)) {
            return isDesc ? broadcastResult.totalSales.desc() : broadcastResult.totalSales.asc();
        } else {
            return isDesc ? broadcastResult.totalViews.desc() : broadcastResult.totalViews.asc();
        }
    }

    // [New Helper 1] 차트용 기간 (Trend): 최근 13일, 최근 1년, 최근 10년
    private BooleanExpression getChartPeriodCondition(String type, com.querydsl.core.types.dsl.DateTimePath<LocalDateTime> path) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        if ("DAILY".equalsIgnoreCase(type)) {
            // 일별 차트: 최근 30일
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

    // [New Helper 2] 랭킹용 기간 (Snapshot): 오늘, 이번 달, 올해
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