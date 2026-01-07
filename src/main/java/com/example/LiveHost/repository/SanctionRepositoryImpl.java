package com.example.LiveHost.repository;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.enums.SanctionType;
import com.example.LiveHost.dto.response.SanctionStatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.jooq.impl.DSL.*;

@RequiredArgsConstructor
public class SanctionRepositoryImpl implements SanctionRepositoryCustom {

    private final DSLContext dsl;

    private final org.jooq.Table<Record> broadcastTable = table(name("broadcast")).as("b");
    private final org.jooq.Table<Record> sanctionTable = table(name("sanction")).as("sc");
    private final org.jooq.Table<Record> sellerTable = table(name("seller")).as("s");
    private final org.jooq.Table<Record> memberTable = table(name("member")).as("m");

    private final Field<String> broadcastStatus = field(name("b", "status"), String.class);
    private final Field<LocalDateTime> broadcastEndedAt = field(name("b", "ended_at"), LocalDateTime.class);
    private final Field<Long> sellerId = field(name("s", "seller_id"), Long.class);
    private final Field<String> sellerName = field(name("s", "name"), String.class);
    private final Field<String> sellerLoginId = field(name("s", "login_id"), String.class);

    private final Field<Long> sanctionId = field(name("sc", "sanction_id"), Long.class);
    private final Field<Long> sanctionMemberId = field(name("sc", "member_id"), Long.class);
    private final Field<String> sanctionStatus = field(name("sc", "status"), String.class);
    private final Field<LocalDateTime> sanctionCreatedAt = field(name("sc", "created_at"), LocalDateTime.class);

    private final Field<String> memberName = field(name("m", "name"), String.class);

    // =================================================================================
    // 1. 차트 데이터 (Chart)
    // =================================================================================

    // 1-1. 판매자 강제 종료 차트 (Broadcast 테이블: status = STOPPED)
    @Override
    public List<SanctionStatisticsResponse.ChartData> getSellerForceStopChart(String periodType) {
        Field<String> dateExpr = getDateExpression(periodType, broadcastEndedAt);

        return dsl.select(dateExpr, count())
                .from(broadcastTable)
                .where(
                        broadcastStatus.eq(BroadcastStatus.STOPPED.name()),
                        getChartPeriodCondition(periodType, broadcastEndedAt)
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch(record -> new SanctionStatisticsResponse.ChartData(
                        record.get(dateExpr),
                        record.get(count(), Long.class)
                ));
    }

    // 1-2. 시청자 제재 차트 (Sanction 테이블: 모든 제재 건수)
    @Override
    public List<SanctionStatisticsResponse.ChartData> getViewerSanctionChart(String periodType) {
        Field<String> dateExpr = getDateExpression(periodType, sanctionCreatedAt);

        return dsl.select(dateExpr, count())
                .from(sanctionTable)
                .where(
                        sanctionStatus.in(SanctionType.MUTE.name(), SanctionType.OUT.name()),
                        getChartPeriodCondition(periodType, sanctionCreatedAt)
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch(record -> new SanctionStatisticsResponse.ChartData(
                        record.get(dateExpr),
                        record.get(count(), Long.class)
                ));
    }

    // =================================================================================
    // 2. 랭킹 데이터 (Ranking)
    // =================================================================================

    // 2-1. 판매자 강제 종료 순위 (Broadcast 테이블 -> Seller 조인)
    @Override
    public List<SanctionStatisticsResponse.SellerRank> getSellerForceStopRanking(String periodType, int limit) {
        return dsl.select(sellerId, sellerName, sellerLoginId, count())
                .from(broadcastTable)
                .join(sellerTable).on(field(name("b", "seller_id"), Long.class).eq(sellerId))
                .where(
                        broadcastStatus.eq(BroadcastStatus.STOPPED.name()),
                        getRankingPeriodCondition(periodType, broadcastEndedAt)
                )
                .groupBy(sellerId, sellerName, sellerLoginId)
                .orderBy(count().desc())
                .limit(limit)
                .fetch(this::mapSellerRank);
    }

    // 2-2. 시청자 제재 순위 (Sanction 테이블 -> MemberId 기준)
    @Override
    public List<SanctionStatisticsResponse.ViewerRank> getViewerSanctionRanking(String periodType, int limit) {
        return dsl.select(sanctionMemberId, memberName, count())
                .from(sanctionTable)
                .leftJoin(memberTable).on(field(name("m", "member_id"), Long.class).eq(sanctionMemberId))
                .where(
                        getRankingPeriodCondition(periodType, sanctionCreatedAt)
                )
                .groupBy(sanctionMemberId, memberName)
                .orderBy(count().desc())
                .limit(limit)
                .fetch(this::mapViewerRank);
    }

    @Override
    public SanctionTypeResult findLatestSanction(Long broadcastIdValue, Long memberIdValue) {
        Record record = dsl.select(sanctionId, sanctionStatus)
                .from(sanctionTable)
                .where(
                        field(name("sc", "broadcast_id"), Long.class).eq(broadcastIdValue),
                        sanctionMemberId.eq(memberIdValue)
                )
                .orderBy(sanctionCreatedAt.desc())
                .limit(1)
                .fetchOne();

        if (record == null) {
            return null;
        }
        return new SanctionTypeResult(
                record.get(sanctionId),
                record.get(sanctionStatus)
        );
    }

    // Helper Method
    // [Helper 1] 날짜 포맷 변환 (Group By용)
    private Field<String> getDateExpression(String periodType, Field<LocalDateTime> datePath) {
        String format = "DAILY".equalsIgnoreCase(periodType) ? "%Y-%m-%d" :
                "MONTHLY".equalsIgnoreCase(periodType) ? "%Y-%m" : "%Y";
        return field("DATE_FORMAT({0}, {1})", String.class, datePath, inline(format));
    }

    // [Helper 2] 차트용 기간 (Trend): 최근 1달, 최근 1년, 최근 10년
    private Condition getChartPeriodCondition(String type, Field<LocalDateTime> path) {
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
        return path.ge(startDate);
    }

    // [Helper 3] 랭킹용 기간 (Snapshot): 오늘 하루, 이번 달, 올해
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
        return path.ge(startDate);
    }

    private SanctionStatisticsResponse.SellerRank mapSellerRank(Record record) {
        return SanctionStatisticsResponse.SellerRank.builder()
                .sellerId(record.get(sellerId))
                .sellerName(record.get(sellerName))
                .phone(record.get(sellerLoginId))
                .sanctionCount(record.get(count(), Long.class))
                .build();
    }

    private SanctionStatisticsResponse.ViewerRank mapViewerRank(Record record) {
        return SanctionStatisticsResponse.ViewerRank.builder()
                .viewerId(record.get(sanctionMemberId) != null ? record.get(sanctionMemberId).toString() : "unknown")
                .name(record.get(memberName) != null ? record.get(memberName) : "Unknown")
                .sanctionCount(record.get(count(), Long.class))
                .build();
    }
}
