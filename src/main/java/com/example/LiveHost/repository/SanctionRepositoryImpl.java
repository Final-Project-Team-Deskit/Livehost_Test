package com.example.LiveHost.repository;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.enums.SanctionType;
import com.example.LiveHost.dto.response.SanctionStatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@RequiredArgsConstructor
public class SanctionRepositoryImpl implements SanctionRepositoryCustom {

    private static final Table<?> BROADCAST = DSL.table(DSL.name("broadcast"));
    private static final Table<?> SANCTION = DSL.table(DSL.name("sanction"));
    private static final Table<?> MEMBER = DSL.table(DSL.name("member"));
    private static final Table<?> SELLER = DSL.table(DSL.name("seller"));

    private static final Field<Long> BROADCAST_ID = DSL.field(DSL.name("broadcast", "broadcast_id"), Long.class);
    private static final Field<String> BROADCAST_STATUS = DSL.field(DSL.name("broadcast", "status"), String.class);
    private static final Field<LocalDateTime> BROADCAST_ENDED_AT = DSL.field(DSL.name("broadcast", "ended_at"), LocalDateTime.class);
    private static final Field<Long> BROADCAST_SELLER_ID = DSL.field(DSL.name("broadcast", "seller_id"), Long.class);

    private static final Field<Long> SANCTION_ID = DSL.field(DSL.name("sanction", "sanction_id"), Long.class);
    private static final Field<Long> SANCTION_MEMBER_ID = DSL.field(DSL.name("sanction", "member_id"), Long.class);
    private static final Field<String> SANCTION_STATUS = DSL.field(DSL.name("sanction", "status"), String.class);
    private static final Field<LocalDateTime> SANCTION_CREATED_AT = DSL.field(DSL.name("sanction", "created_at"), LocalDateTime.class);

    private static final Field<Long> SELLER_ID = DSL.field(DSL.name("seller", "seller_id"), Long.class);
    private static final Field<String> SELLER_NAME = DSL.field(DSL.name("seller", "name"), String.class);
    private static final Field<String> SELLER_PHONE = DSL.field(DSL.name("seller", "phone"), String.class);

    private static final Field<Long> MEMBER_ID = DSL.field(DSL.name("member", "member_id"), Long.class);
    private static final Field<String> MEMBER_NAME = DSL.field(DSL.name("member", "name"), String.class);

    private final DSLContext dsl;

    @Override
    public List<SanctionStatisticsResponse.ChartData> getSellerForceStopChart(String periodType) {
        Field<String> dateExpr = getDateExpression(periodType, BROADCAST_ENDED_AT);
        Field<Long> countField = DSL.count().cast(Long.class).as("count");

        return dsl
                .select(dateExpr, countField)
                .from(BROADCAST)
                .where(
                        BROADCAST_STATUS.eq(BroadcastStatus.STOPPED.name()),
                        getChartPeriodCondition(periodType, BROADCAST_ENDED_AT)
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch(record -> new SanctionStatisticsResponse.ChartData(
                        record.get(dateExpr),
                        record.get(countField)
                ));
    }

    @Override
    public List<SanctionStatisticsResponse.ChartData> getViewerSanctionChart(String periodType) {
        Field<String> dateExpr = getDateExpression(periodType, SANCTION_CREATED_AT);
        Field<Long> countField = DSL.count(SANCTION_ID).cast(Long.class).as("count");

        return dsl
                .select(dateExpr, countField)
                .from(SANCTION)
                .where(
                        SANCTION_STATUS.in(SanctionType.MUTE.name(), SanctionType.OUT.name()),
                        getChartPeriodCondition(periodType, SANCTION_CREATED_AT)
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch(record -> new SanctionStatisticsResponse.ChartData(
                        record.get(dateExpr),
                        record.get(countField)
                ));
    }

    @Override
    public List<SanctionStatisticsResponse.SellerRank> getSellerForceStopRanking(String periodType, int limit) {
        Field<Long> countField = DSL.count().cast(Long.class).as("count");

        return dsl
                .select(SELLER_ID, SELLER_NAME, SELLER_PHONE, countField)
                .from(BROADCAST)
                .join(SELLER).on(BROADCAST_SELLER_ID.eq(SELLER_ID))
                .where(
                        BROADCAST_STATUS.eq(BroadcastStatus.STOPPED.name()),
                        getRankingPeriodCondition(periodType, BROADCAST_ENDED_AT)
                )
                .groupBy(SELLER_ID, SELLER_NAME, SELLER_PHONE)
                .orderBy(countField.desc())
                .limit(limit)
                .fetch(this::toSellerRank);
    }

    @Override
    public List<SanctionStatisticsResponse.ViewerRank> getViewerSanctionRanking(String periodType, int limit) {
        Field<Long> countField = DSL.count(SANCTION_ID).cast(Long.class).as("count");
        Field<String> viewerNameField = DSL.coalesce(MEMBER_NAME, DSL.inline("Unknown")).as("viewer_name");

        return dsl
                .select(SANCTION_MEMBER_ID, viewerNameField, countField)
                .from(SANCTION)
                .leftJoin(MEMBER).on(SANCTION_MEMBER_ID.eq(MEMBER_ID))
                .where(getRankingPeriodCondition(periodType, SANCTION_CREATED_AT))
                .groupBy(SANCTION_MEMBER_ID, viewerNameField)
                .orderBy(countField.desc())
                .limit(limit)
                .fetch(this::toViewerRank);
    }

    private SanctionStatisticsResponse.SellerRank toSellerRank(Record record) {
        return SanctionStatisticsResponse.SellerRank.builder()
                .sellerId(record.get(SELLER_ID))
                .sellerName(record.get(SELLER_NAME))
                .phone(record.get(SELLER_PHONE))
                .sanctionCount(record.get("count", Long.class))
                .build();
    }

    private SanctionStatisticsResponse.ViewerRank toViewerRank(Record record) {
        return SanctionStatisticsResponse.ViewerRank.builder()
                .viewerId(record.get(SANCTION_MEMBER_ID) != null ? record.get(SANCTION_MEMBER_ID).toString() : null)
                .name(record.get("viewer_name", String.class))
                .sanctionCount(record.get("count", Long.class))
                .build();
    }

    private Field<String> getDateExpression(String periodType, Field<LocalDateTime> dateField) {
        String format = "DAILY".equalsIgnoreCase(periodType) ? "%Y-%m-%d" :
                "MONTHLY".equalsIgnoreCase(periodType) ? "%Y-%m" : "%Y";
        return DSL.field("DATE_FORMAT({0}, {1})", String.class, dateField, DSL.inline(format));
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
