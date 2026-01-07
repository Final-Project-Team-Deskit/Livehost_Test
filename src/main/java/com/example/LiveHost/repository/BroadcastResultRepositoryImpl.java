package com.example.LiveHost.repository;

import com.example.LiveHost.dto.response.StatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class BroadcastResultRepositoryImpl implements BroadcastResultRepositoryCustom {

    private static final Table<Record> BROADCAST = DSL.table(DSL.name("broadcast")).as("b");
    private static final Table<Record> BROADCAST_RESULT = DSL.table(DSL.name("broadcast_result")).as("br");

    private static final Field<Long> BROADCAST_ID = DSL.field(DSL.name("b", "broadcast_id"), Long.class);
    private static final Field<String> BROADCAST_TITLE = DSL.field(DSL.name("b", "broadcast_title"), String.class);
    private static final Field<Long> BROADCAST_SELLER_ID = DSL.field(DSL.name("b", "seller_id"), Long.class);
    private static final Field<LocalDateTime> BROADCAST_STARTED_AT = DSL.field(DSL.name("b", "started_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> BROADCAST_ENDED_AT = DSL.field(DSL.name("b", "ended_at"), LocalDateTime.class);

    private static final Field<Long> BROADCAST_RESULT_BROADCAST_ID = DSL.field(DSL.name("br", "broadcast_id"), Long.class);
    private static final Field<BigDecimal> TOTAL_SALES = DSL.field(DSL.name("br", "total_sales"), BigDecimal.class);
    private static final Field<Integer> TOTAL_VIEWS = DSL.field(DSL.name("br", "total_views"), Integer.class);

    private final DSLContext dsl;

    @Override
    public List<StatisticsResponse.ChartData> getSalesChart(Long sellerId, String periodType) {
        Field<String> dateExpr = getDateExpression(periodType);
        Field<BigDecimal> sumSales = DSL.sum(TOTAL_SALES).as("sum_sales");

        return dsl
                .select(
                        dateExpr,
                        sumSales
                )
                .from(BROADCAST_RESULT)
                .join(BROADCAST).on(BROADCAST_ID.eq(BROADCAST_RESULT_BROADCAST_ID))
                .where(buildChartConditions(sellerId, periodType))
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch(record -> new StatisticsResponse.ChartData(
                        record.get(dateExpr),
                        record.get(sumSales)
                ));
    }

    @Override
    public List<StatisticsResponse.ChartData> getArpuChart(Long sellerId, String periodType) {
        Field<String> dateExpr = getDateExpression(periodType);
        Field<BigDecimal> sumSales = DSL.sum(TOTAL_SALES).as("sum_sales");
        Field<BigDecimal> sumViews = DSL.sum(TOTAL_VIEWS).cast(BigDecimal.class).as("sum_views");
        Field<BigDecimal> arpu = sumSales.divide(DSL.nullif(sumViews, BigDecimal.ZERO));

        return dsl
                .select(dateExpr, arpu)
                .from(BROADCAST_RESULT)
                .join(BROADCAST).on(BROADCAST_ID.eq(BROADCAST_RESULT_BROADCAST_ID))
                .where(buildChartConditions(sellerId, periodType))
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch(record -> new StatisticsResponse.ChartData(
                        record.get(dateExpr),
                        record.get(arpu)
                ));
    }

    @Override
    public List<StatisticsResponse.BroadcastRank> getRanking(Long sellerId, String periodType, String sortField, boolean isDesc, int limit) {
        return dsl
                .select(
                        BROADCAST_ID,
                        BROADCAST_TITLE,
                        TOTAL_SALES,
                        TOTAL_VIEWS
                )
                .from(BROADCAST_RESULT)
                .join(BROADCAST).on(BROADCAST_ID.eq(BROADCAST_RESULT_BROADCAST_ID))
                .where(buildRankingConditions(sellerId, periodType))
                .orderBy(getOrderSpecifier(sortField, isDesc))
                .limit(limit)
                .fetch(record -> {
                    Integer views = record.get(TOTAL_VIEWS);
                    return StatisticsResponse.BroadcastRank.builder()
                            .broadcastId(record.get(BROADCAST_ID))
                            .title(record.get(BROADCAST_TITLE))
                            .totalSales(record.get(TOTAL_SALES))
                            .totalViews(views != null ? views : 0)
                            .build();
                });
    }

    private List<Condition> buildChartConditions(Long sellerId, String periodType) {
        List<Condition> conditions = new ArrayList<>();
        if (sellerId != null) {
            conditions.add(BROADCAST_SELLER_ID.eq(sellerId));
        }
        conditions.add(BROADCAST_ENDED_AT.isNotNull());
        conditions.add(getChartPeriodCondition(periodType, BROADCAST_STARTED_AT));
        return conditions;
    }

    private List<Condition> buildRankingConditions(Long sellerId, String periodType) {
        List<Condition> conditions = new ArrayList<>();
        if (sellerId != null) {
            conditions.add(BROADCAST_SELLER_ID.eq(sellerId));
        }
        conditions.add(BROADCAST_ENDED_AT.isNotNull());
        conditions.add(getRankingPeriodCondition(periodType, BROADCAST_STARTED_AT));
        return conditions;
    }

    private Field<String> getDateExpression(String periodType) {
        String format = "DAILY".equalsIgnoreCase(periodType) ? "%Y-%m-%d" :
                "MONTHLY".equalsIgnoreCase(periodType) ? "%Y-%m" : "%Y";
        return DSL.field("DATE_FORMAT({0}, {1})", String.class, BROADCAST_STARTED_AT, DSL.inline(format));
    }

    private SortField<?> getOrderSpecifier(String sortField, boolean isDesc) {
        Field<?> field = "SALES".equalsIgnoreCase(sortField) ? TOTAL_SALES : TOTAL_VIEWS;
        return isDesc ? field.desc().nullsLast() : field.asc().nullsLast();
    }

    private Condition getChartPeriodCondition(String type, Field<LocalDateTime> path) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        if ("DAILY".equalsIgnoreCase(type)) {
            startDate = now.minusDays(6).with(LocalTime.MIN);
        } else if ("MONTHLY".equalsIgnoreCase(type)) {
            startDate = now.minusMonths(11).withDayOfMonth(1).with(LocalTime.MIN);
        } else {
            startDate = now.minusYears(4).withDayOfYear(1).with(LocalTime.MIN);
        }
        return path.ge(startDate);
    }

    private Condition getRankingPeriodCondition(String type, Field<LocalDateTime> path) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        if ("DAILY".equalsIgnoreCase(type)) {
            startDate = now.with(LocalTime.MIN);
        } else if ("MONTHLY".equalsIgnoreCase(type)) {
            startDate = now.withDayOfMonth(1).with(LocalTime.MIN);
        } else {
            startDate = now.with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN);
        }
        return path.ge(startDate);
    }
}
