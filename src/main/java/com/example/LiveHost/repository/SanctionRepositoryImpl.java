package com.example.LiveHost.repository;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.enums.SanctionType;
import com.example.LiveHost.dto.response.SanctionStatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class SanctionRepositoryImpl implements SanctionRepositoryCustom {

    private static final Table<Record> BROADCAST = DSL.table(DSL.name("broadcast")).as("b");
    private static final Table<Record> SANCTION = DSL.table(DSL.name("sanction")).as("sc");
    private static final Table<Record> MEMBER = DSL.table(DSL.name("member")).as("m");
    private static final Table<Record> SELLER = DSL.table(DSL.name("seller")).as("s");

    private static final Field<Long> BROADCAST_ID = DSL.field(DSL.name("b", "broadcast_id"), Long.class);
    private static final Field<BroadcastStatus> BROADCAST_STATUS = DSL.field(DSL.name("b", "status"), BroadcastStatus.class);
    private static final Field<LocalDateTime> BROADCAST_ENDED_AT = DSL.field(DSL.name("b", "ended_at"), LocalDateTime.class);
    private static final Field<Long> BROADCAST_SELLER_ID = DSL.field(DSL.name("b", "seller_id"), Long.class);

    private static final Field<Long> SANCTION_ID = DSL.field(DSL.name("sc", "sanction_id"), Long.class);
    private static final Field<Long> SANCTION_MEMBER_ID = DSL.field(DSL.name("sc", "member_id"), Long.class);
    private static final Field<LocalDateTime> SANCTION_CREATED_AT = DSL.field(DSL.name("sc", "created_at"), LocalDateTime.class);
    private static final Field<SanctionType> SANCTION_STATUS = DSL.field(DSL.name("sc", "status"), SanctionType.class);

    private static final Field<Long> SELLER_ID = DSL.field(DSL.name("s", "seller_id"), Long.class);
    private static final Field<String> SELLER_NAME = DSL.field(DSL.name("s", "name"), String.class);
    private static final Field<String> SELLER_LOGIN_ID = DSL.field(DSL.name("s", "login_id"), String.class);

    private static final Field<String> MEMBER_NAME = DSL.field(DSL.name("m", "name"), String.class);

    private final DSLContext dsl;

    @Override
    public List<SanctionStatisticsResponse.ChartData> getSellerForceStopChart(String periodType) {
        Field<String> dateExpr = getDateExpression(periodType, BROADCAST_ENDED_AT);
        Field<Long> stopCount = DSL.count(BROADCAST_ID).as("stop_count");

        return dsl
                .select(dateExpr, stopCount)
                .from(BROADCAST)
                .where(
                        BROADCAST_STATUS.eq(BroadcastStatus.STOPPED),
                        getChartPeriodCondition(periodType, BROADCAST_ENDED_AT)
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch(record -> new SanctionStatisticsResponse.ChartData(
                        record.get(dateExpr),
                        record.get(stopCount)
                ));
    }

    @Override
    public List<SanctionStatisticsResponse.ChartData> getViewerSanctionChart(String periodType) {
        Field<String> dateExpr = getDateExpression(periodType, SANCTION_CREATED_AT);
        Field<Long> sanctionCount = DSL.count(SANCTION_ID).as("sanction_count");

        return dsl
                .select(dateExpr, sanctionCount)
                .from(SANCTION)
                .where(
                        SANCTION_STATUS.in(SanctionType.MUTE, SanctionType.OUT),
                        getChartPeriodCondition(periodType, SANCTION_CREATED_AT)
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch(record -> new SanctionStatisticsResponse.ChartData(
                        record.get(dateExpr),
                        record.get(sanctionCount)
                ));
    }

    @Override
    public List<SanctionStatisticsResponse.SellerRank> getSellerForceStopRanking(String periodType, int limit) {
        Field<Long> stopCount = DSL.count(BROADCAST_ID).as("stop_count");

        return dsl
                .select(
                        SELLER_ID,
                        SELLER_NAME,
                        SELLER_LOGIN_ID,
                        stopCount
                )
                .from(BROADCAST)
                .join(SELLER).on(BROADCAST_SELLER_ID.eq(SELLER_ID))
                .where(
                        BROADCAST_STATUS.eq(BroadcastStatus.STOPPED),
                        getRankingPeriodCondition(periodType, BROADCAST_ENDED_AT)
                )
                .groupBy(SELLER_ID, SELLER_NAME, SELLER_LOGIN_ID)
                .orderBy(stopCount.desc())
                .limit(limit)
                .fetch(record -> new SanctionStatisticsResponse.SellerRank(
                        record.get(SELLER_ID),
                        record.get(SELLER_NAME),
                        record.get(SELLER_LOGIN_ID),
                        record.get(stopCount)
                ));
    }

    @Override
    public List<SanctionStatisticsResponse.ViewerRank> getViewerSanctionRanking(String periodType, int limit) {
        Field<Long> sanctionCount = DSL.count(SANCTION_ID).as("sanction_count");
        Field<String> viewerName = DSL.coalesce(MEMBER_NAME, DSL.inline("Unknown")).as("viewer_name");

        return dsl
                .select(
                        SANCTION_MEMBER_ID,
                        viewerName,
                        sanctionCount
                )
                .from(SANCTION)
                .leftJoin(MEMBER).on(SANCTION_MEMBER_ID.eq(DSL.field(DSL.name("m", "member_id"), Long.class)))
                .where(getRankingPeriodCondition(periodType, SANCTION_CREATED_AT))
                .groupBy(SANCTION_MEMBER_ID, MEMBER_NAME)
                .orderBy(sanctionCount.desc())
                .limit(limit)
                .fetch(record -> new SanctionStatisticsResponse.ViewerRank(
                        record.get(SANCTION_MEMBER_ID) != null ? record.get(SANCTION_MEMBER_ID).toString() : null,
                        record.get(viewerName),
                        record.get(sanctionCount)
                ));
    }

    private Field<String> getDateExpression(String periodType, Field<LocalDateTime> dateField) {
        String format = "DAILY".equalsIgnoreCase(periodType) ? "%Y-%m-%d" :
                "MONTHLY".equalsIgnoreCase(periodType) ? "%Y-%m" : "%Y";
        return DSL.field("DATE_FORMAT({0}, {1})", String.class, dateField, DSL.inline(format));
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
