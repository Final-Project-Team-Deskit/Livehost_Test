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
import java.util.List;

@RequiredArgsConstructor
public class BroadcastResultRepositoryImpl implements BroadcastResultRepositoryCustom {

    private static final Table<?> BROADCAST = DSL.table(DSL.name("broadcast"));
    private static final Table<?> BROADCAST_RESULT = DSL.table(DSL.name("broadcast_result"));

    private static final Field<Long> BROADCAST_ID = DSL.field(DSL.name("broadcast", "broadcast_id"), Long.class);
    private static final Field<String> BROADCAST_TITLE = DSL.field(DSL.name("broadcast", "broadcast_title"), String.class);
    private static final Field<Long> BROADCAST_SELLER_ID = DSL.field(DSL.name("broadcast", "seller_id"), Long.class);
    private static final Field<LocalDateTime> BROADCAST_STARTED_AT = DSL.field(DSL.name("broadcast", "started_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> BROADCAST_ENDED_AT = DSL.field(DSL.name("broadcast", "ended_at"), LocalDateTime.class);

    private static final Field<BigDecimal> TOTAL_SALES = DSL.field(DSL.name("broadcast_result", "total_sales"), BigDecimal.class);
    private static final Field<Integer> TOTAL_VIEWS = DSL.field(DSL.name("broadcast_result", "total_views"), Integer.class);

    private final DSLContext dsl;

    @Override
    public List<StatisticsResponse.ChartData> getSalesChart(Long sellerId, String periodType) {
        Field<String> dateExpr = getDateExpression(periodType, BROADCAST_STARTED_AT);
        Field<BigDecimal> totalSalesSum = DSL.sum(TOTAL_SALES).as("total_sales");

        return dsl
                .select(dateExpr, totalSalesSum)
                .from(BROADCAST_RESULT)
                .join(BROADCAST).on(BROADCAST_ID.eq(DSL.field(DSL.name("broadcast_result", "broadcast_id"), Long.class)))
                .where(
                        sellerIdEq(sellerId),
                        BROADCAST_ENDED_AT.isNotNull(),
                        getChartPeriodCondition(periodType, BROADCAST_STARTED_AT)
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch(record -> new StatisticsResponse.ChartData(
                        record.get(dateExpr),
                        record.get(totalSalesSum)
                ));
    }

    @Override
    public List<StatisticsResponse.ChartData> getArpuChart(Long sellerId, String periodType) {
        Field<String> dateExpr = getDateExpression(periodType, BROADCAST_STARTED_AT);
        Field<BigDecimal> arpu = DSL.sum(TOTAL_SALES)
                .divide(DSL.nullif(DSL.sum(TOTAL_VIEWS), 0))
                .as("arpu");

        return dsl
                .select(dateExpr, arpu)
                .from(BROADCAST_RESULT)
                .join(BROADCAST).on(BROADCAST_ID.eq(DSL.field(DSL.name("broadcast_result", "broadcast_id"), Long.class)))
                .where(
                        sellerIdEq(sellerId),
                        BROADCAST_ENDED_AT.isNotNull(),
                        getChartPeriodCondition(periodType, BROADCAST_STARTED_AT)
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch(record -> new StatisticsResponse.ChartData(
                        record.get(dateExpr),
                        record.get(arpu)
                ));
    }

    @Override
    public List<StatisticsResponse.BroadcastRank> getRanking(Long sellerId, String periodType, String sortField, boolean isDesc, int limit) {
        SortField<?> orderField = getOrderField(sortField, isDesc);

        return dsl
                .select(BROADCAST_ID, BROADCAST_TITLE, TOTAL_SALES, TOTAL_VIEWS)
                .from(BROADCAST_RESULT)
                .join(BROADCAST).on(BROADCAST_ID.eq(DSL.field(DSL.name("broadcast_result", "broadcast_id"), Long.class)))
                .where(
                        sellerIdEq(sellerId),
                        BROADCAST_ENDED_AT.isNotNull(),
                        getRankingPeriodCondition(periodType, BROADCAST_STARTED_AT)
                )
                .orderBy(orderField)
                .limit(limit)
                .fetch(this::toBroadcastRank);
    }

    private StatisticsResponse.BroadcastRank toBroadcastRank(Record record) {
        return StatisticsResponse.BroadcastRank.builder()
                .broadcastId(record.get(BROADCAST_ID))
                .title(record.get(BROADCAST_TITLE))
                .totalSales(record.get(TOTAL_SALES))
                .totalViews(record.get(TOTAL_VIEWS) != null ? record.get(TOTAL_VIEWS) : 0)
                .build();
    }

    private Condition sellerIdEq(Long sellerId) {
        return sellerId != null ? BROADCAST_SELLER_ID.eq(sellerId) : DSL.trueCondition();
    }

    private Field<String> getDateExpression(String periodType, Field<LocalDateTime> dateField) {
        String format = "DAILY".equalsIgnoreCase(periodType) ? "%Y-%m-%d" :
                "MONTHLY".equalsIgnoreCase(periodType) ? "%Y-%m" : "%Y";
        return DSL.field("DATE_FORMAT({0}, {1})", String.class, dateField, DSL.inline(format));
    }

    private SortField<?> getOrderField(String sortField, boolean isDesc) {
        if ("SALES".equalsIgnoreCase(sortField)) {
            return isDesc ? TOTAL_SALES.desc() : TOTAL_SALES.asc();
        }
        return isDesc ? TOTAL_VIEWS.desc() : TOTAL_VIEWS.asc();
    }

    private Condition getChartPeriodCondition(String type, Field<LocalDateTime> dateField) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        if ("DAILY".equalsIgnoreCase(type)) {
            startDate = now.minusDays(6).with(LocalTime.MIN);
        } else if ("MONTHLY".equalsIgnoreCase(type)) {
            startDate = now.minusMonths(11).withDayOfMonth(1).with(LocalTime.MIN);
        } else {
            startDate = now.minusYears(4).withDayOfYear(1).with(LocalTime.MIN);
        }
        return dateField.ge(startDate);
    }

    private Condition getRankingPeriodCondition(String type, Field<LocalDateTime> dateField) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        if ("DAILY".equalsIgnoreCase(type)) {
            startDate = now.with(LocalTime.MIN);
        } else if ("MONTHLY".equalsIgnoreCase(type)) {
            startDate = now.withDayOfMonth(1).with(LocalTime.MIN);
        } else {
            startDate = now.with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN);
        }
        return dateField.ge(startDate);
    }
}
