package com.example.LiveHost.repository;

import com.example.LiveHost.dto.response.StatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.jooq.impl.DSL.*;

@RequiredArgsConstructor
public class BroadcastResultRepositoryImpl implements BroadcastResultRepositoryCustom {

    private final DSLContext dsl;

    private final org.jooq.Table<Record> broadcastTable = table(name("broadcast")).as("b");
    private final org.jooq.Table<Record> resultTable = table(name("broadcast_result")).as("br");

    private final Field<Long> broadcastId = field(name("b", "broadcast_id"), Long.class);
    private final Field<Long> sellerId = field(name("b", "seller_id"), Long.class);
    private final Field<String> broadcastTitle = field(name("b", "broadcast_title"), String.class);
    private final Field<LocalDateTime> startedAt = field(name("b", "started_at"), LocalDateTime.class);
    private final Field<LocalDateTime> endedAt = field(name("b", "ended_at"), LocalDateTime.class);

    private final Field<BigDecimal> totalSales = field(name("br", "total_sales"), BigDecimal.class);
    private final Field<Integer> totalViews = field(name("br", "total_views"), Integer.class);

    // 1. 기간별 매출 차트 조회
    @Override
    public List<StatisticsResponse.ChartData> getSalesChart(Long sellerId, String periodType) {
        Field<String> dateExpr = getDateExpression(periodType);
        Field<BigDecimal> totalSalesSum = sum(totalSales);

        return dsl.select(dateExpr, totalSalesSum)
                .from(resultTable)
                .join(broadcastTable).on(field(name("br", "broadcast_id"), Long.class).eq(broadcastId))
                .where(
                        sellerIdEq(sellerId),
                        endedAt.isNotNull(),
                        getChartPeriodCondition(periodType, startedAt)
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch(record -> new StatisticsResponse.ChartData(
                        record.get(dateExpr),
                        record.get(totalSalesSum) != null ? record.get(totalSalesSum) : BigDecimal.ZERO
                ));
    }

    // 2. 기간별 ARPU(객단가) 차트 조회
    @Override
    public List<StatisticsResponse.ChartData> getArpuChart(Long sellerId, String periodType) {
        Field<String> dateExpr = getDateExpression(periodType);

        Field<BigDecimal> arpu = sum(totalSales).div(nullif(sum(totalViews), 0));

        return dsl.select(dateExpr, arpu)
                .from(resultTable)
                .join(broadcastTable).on(field(name("br", "broadcast_id"), Long.class).eq(broadcastId))
                .where(
                        sellerIdEq(sellerId),
                        endedAt.isNotNull(),
                        getChartPeriodCondition(periodType, startedAt)
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch(record -> new StatisticsResponse.ChartData(
                        record.get(dateExpr),
                        record.get(arpu) != null ? record.get(arpu) : BigDecimal.ZERO
                ));
    }

    // 3. 방송 랭킹 조회 (메서드명 getRanking으로 통일)
    @Override
    public List<StatisticsResponse.BroadcastRank> getRanking(Long sellerId, String periodType, String sortField, boolean isDesc, int limit) {
        return dsl.select(broadcastId, broadcastTitle, totalSales, totalViews)
                .from(resultTable)
                .join(broadcastTable).on(field(name("br", "broadcast_id"), Long.class).eq(broadcastId))
                .where(
                        sellerIdEq(sellerId),
                        endedAt.isNotNull(),
                        getRankingPeriodCondition(periodType, startedAt)
                )
                .orderBy(getOrderSpecifier(sortField, isDesc))
                .limit(limit)
                .fetch(this::mapRank);
    }

    // --- Helper Methods ---
    private Condition sellerIdEq(Long sellerIdValue) {
        return sellerIdValue != null ? sellerId.eq(sellerIdValue) : trueCondition();
    }

    private Field<String> getDateExpression(String periodType) {
        String format = "DAILY".equalsIgnoreCase(periodType) ? "%Y-%m-%d" :
                "MONTHLY".equalsIgnoreCase(periodType) ? "%Y-%m" : "%Y";
        return field("DATE_FORMAT({0}, {1})", String.class, startedAt, inline(format));
    }

    private org.jooq.SortField<?> getOrderSpecifier(String sortField, boolean isDesc) {
        if ("SALES".equalsIgnoreCase(sortField)) {
            return isDesc ? totalSales.desc() : totalSales.asc();
        } else {
            return isDesc ? totalViews.desc() : totalViews.asc();
        }
    }

    // [New Helper 1] 차트용 기간 (Trend): 최근 13일, 최근 1년, 최근 10년
    private Condition getChartPeriodCondition(String type, Field<LocalDateTime> path) {
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
    private Condition getRankingPeriodCondition(String type, Field<LocalDateTime> path) {
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

    private StatisticsResponse.BroadcastRank mapRank(Record record) {
        return StatisticsResponse.BroadcastRank.builder()
                .broadcastId(record.get(broadcastId))
                .title(record.get(broadcastTitle))
                .totalSales(record.get(totalSales))
                .totalViews(record.get(totalViews, Integer.class) != null ? record.get(totalViews) : 0)
                .build();
    }
}
