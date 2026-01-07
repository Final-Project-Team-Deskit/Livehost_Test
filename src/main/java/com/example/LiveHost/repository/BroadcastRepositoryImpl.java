package com.example.LiveHost.repository;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.enums.VodStatus;
import com.example.LiveHost.dto.response.BroadcastListResponse;
import com.example.LiveHost.dto.request.BroadcastSearch;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.Table;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.jooq.impl.DSL.*;

@RequiredArgsConstructor
public class BroadcastRepositoryImpl implements BroadcastRepositoryCustom {

    private final DSLContext dsl;

    private final Table<Record> broadcastTable = table(name("broadcast")).as("b");
    private final Table<Record> sellerTable = table(name("seller")).as("s");
    private final Table<Record> tagCategoryTable = table(name("tag_category")).as("t");
    private final Table<Record> broadcastResultTable = table(name("broadcast_result")).as("br");
    private final Table<Record> vodTable = table(name("vod")).as("v");
    private final Table<Record> sanctionTable = table(name("sanction")).as("sc");

    private final Field<Long> broadcastId = field(name("b", "broadcast_id"), Long.class);
    private final Field<String> broadcastTitle = field(name("b", "broadcast_title"), String.class);
    private final Field<String> broadcastNotice = field(name("b", "broadcast_notice"), String.class);
    private final Field<String> broadcastThumbUrl = field(name("b", "broadcast_thumb_url"), String.class);
    private final Field<String> broadcastStatus = field(name("b", "status"), String.class);
    private final Field<LocalDateTime> scheduledAt = field(name("b", "scheduled_at"), LocalDateTime.class);
    private final Field<LocalDateTime> startedAt = field(name("b", "started_at"), LocalDateTime.class);
    private final Field<LocalDateTime> endedAt = field(name("b", "ended_at"), LocalDateTime.class);
    private final Field<Long> broadcastSellerId = field(name("b", "seller_id"), Long.class);
    private final Field<Long> broadcastCategoryId = field(name("b", "tag_category_id"), Long.class);

    private final Field<String> sellerName = field(name("s", "name"), String.class);
    private final Field<String> tagCategoryName = field(name("t", "tag_category_name"), String.class);

    private final Field<Integer> totalViews = field(name("br", "total_views"), Integer.class);
    private final Field<Integer> totalLikes = field(name("br", "total_likes"), Integer.class);
    private final Field<java.math.BigDecimal> totalSales = field(name("br", "total_sales"), java.math.BigDecimal.class);

    private final Field<String> vodStatus = field(name("v", "status"), String.class);
    private final Field<Long> sanctionId = field(name("sc", "sanction_id"), Long.class);

    @Override
    public Slice<BroadcastListResponse> searchBroadcasts(Long sellerId, BroadcastSearch condition, Pageable pageable, boolean isAdmin) {
        Field<Long> reportCount = countDistinct(sanctionId).cast(Long.class).as("report_count");

        List<BroadcastListResponse> content = dsl.select(
                        broadcastId, broadcastTitle, broadcastNotice,
                        sellerName, tagCategoryName, broadcastThumbUrl,
                        broadcastStatus, scheduledAt, startedAt, endedAt,
                        totalViews, vodStatus, reportCount, totalSales, totalLikes
                )
                .from(broadcastTable)
                .join(sellerTable).on(field(name("s", "seller_id"), Long.class).eq(broadcastSellerId))
                .join(tagCategoryTable).on(field(name("t", "tag_category_id"), Long.class).eq(broadcastCategoryId))
                .leftJoin(broadcastResultTable).on(field(name("br", "broadcast_id"), Long.class).eq(broadcastId))
                .leftJoin(vodTable).on(field(name("v", "broadcast_id"), Long.class).eq(broadcastId))
                .leftJoin(sanctionTable).on(field(name("sc", "broadcast_id"), Long.class).eq(broadcastId))
                .where(
                        sellerIdEq(sellerId),
                        tabCondition(condition.getTab()),
                        keywordContains(condition.getKeyword()),
                        categoryEq(condition.getCategoryId()),
                        dateBetween(condition.getStartDate(), condition.getEndDate()),
                        statusDetailFilter(condition.getStatusFilter()),
                        publicFilter(condition.getIsPublic()),
                        publicCondition(isAdmin),
                        broadcastStatus.ne(BroadcastStatus.DELETED.name())
                )
                .groupBy(broadcastId, broadcastTitle, broadcastNotice, sellerName, tagCategoryName, broadcastThumbUrl,
                        broadcastStatus, scheduledAt, startedAt, endedAt, totalViews, vodStatus, totalSales, totalLikes)
                .orderBy(getOrderSpecifier(condition, reportCount))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch(this::mapBroadcastList);

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }
        return new SliceImpl<>(content, pageable, hasNext);
    }

    // 2. Overview 조회 (Tab=ALL, Top 5)
    @Override
    public List<BroadcastListResponse> findTop5ByStatus(Long sellerId, List<BroadcastStatus> statuses, BroadcastSortOrder sortOrder, boolean isAdmin) {
        Field<Long> reportCount = inline(0L);
        SortField<?> orderField = getOrderSpecifier(sortOrder, reportCount);

        return dsl.select(
                        broadcastId, broadcastTitle, broadcastNotice,
                        sellerName, tagCategoryName, broadcastThumbUrl,
                        broadcastStatus, scheduledAt, startedAt, endedAt,
                        totalViews, vodStatus, reportCount, totalSales, totalLikes
                )
                .from(broadcastTable)
                .join(sellerTable).on(field(name("s", "seller_id"), Long.class).eq(broadcastSellerId))
                .join(tagCategoryTable).on(field(name("t", "tag_category_id"), Long.class).eq(broadcastCategoryId))
                .leftJoin(broadcastResultTable).on(field(name("br", "broadcast_id"), Long.class).eq(broadcastId))
                .leftJoin(vodTable).on(field(name("v", "broadcast_id"), Long.class).eq(broadcastId))
                .where(
                        sellerIdEq(sellerId),
                        broadcastStatus.in(statuses.stream().map(Enum::name).toList()),
                        publicCondition(isAdmin),
                        broadcastStatus.ne(BroadcastStatus.DELETED.name())
                )
                .orderBy(orderField)
                .limit(5)
                .fetch(this::mapBroadcastList);
    }

    // 3. 시간 슬롯 예약 확인 (플랫폼 전체 3개 제한)
    @Override
    public long countByTimeSlot(LocalDateTime start, LocalDateTime end) {
        Long count = dsl.select(count())
                .from(broadcastTable)
                .where(
                        broadcastStatus.in(BroadcastStatus.RESERVED.name(), BroadcastStatus.READY.name()),
                        scheduledAt.between(start, end),
                        broadcastStatus.ne(BroadcastStatus.DELETED.name())
                )
                .fetchOne(0, Long.class);
        return count != null ? count : 0L;
    }

    // --- Helper Methods (누락된 메서드 구현) ---

    @Override
    public List<Long> findBroadcastIdsForReadyTransition(LocalDateTime now) {
        LocalDateTime start = now;
        LocalDateTime end = now.plusMinutes(10);
        return dsl.select(broadcastId)
                .from(broadcastTable)
                .where(
                        broadcastStatus.eq(BroadcastStatus.RESERVED.name()),
                        scheduledAt.between(start, end),
                        broadcastStatus.ne(BroadcastStatus.DELETED.name())
                )
                .fetch(broadcastId);
    }

    @Override
    public List<Long> findBroadcastIdsForNoShow(LocalDateTime now) {
        LocalDateTime threshold = now.minusMinutes(10);
        return dsl.select(broadcastId)
                .from(broadcastTable)
                .where(
                        broadcastStatus.in(BroadcastStatus.RESERVED.name(), BroadcastStatus.READY.name()),
                        scheduledAt.lessOrEqual(threshold),
                        broadcastStatus.ne(BroadcastStatus.DELETED.name())
                )
                .fetch(broadcastId);
    }

    @Override
    public List<BroadcastScheduleInfo> findBroadcastSchedules(LocalDateTime start, LocalDateTime end, List<BroadcastStatus> statuses) {
        return dsl.select(broadcastId, scheduledAt, broadcastStatus)
                .from(broadcastTable)
                .where(
                        scheduledAt.between(start, end),
                        broadcastStatus.in(statuses.stream().map(Enum::name).toList()),
                        broadcastStatus.ne(BroadcastStatus.DELETED.name())
                )
                .fetch(record -> new BroadcastScheduleInfo(
                        record.get(broadcastId),
                        record.get(scheduledAt),
                        BroadcastStatus.valueOf(record.get(broadcastStatus))
                ));
    }

    private Condition sellerIdEq(Long sellerIdValue) {
        return sellerIdValue != null ? broadcastSellerId.eq(sellerIdValue) : trueCondition();
    }

    private Condition categoryEq(Long categoryIdValue) {
        return categoryIdValue != null ? broadcastCategoryId.eq(categoryIdValue) : trueCondition();
    }

    private Condition keywordContains(String k) {
        return (k != null && !k.isEmpty()) ? broadcastTitle.contains(k) : trueCondition();
    }

    // [날짜 필터] 시작일~종료일 사이에 방송이 시작되었거나 예정된 경우
    private Condition dateBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) return trueCondition();
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.atTime(23, 59, 59);
        return startedAt.between(startAt, endAt)
                .or(scheduledAt.between(startAt, endAt));
    }

    // [권한 필터] 관리자/판매자는 모두 조회, 일반 유저는 공개된 방송만 조회
    private Condition publicCondition(boolean isAdmin) {
        if (isAdmin) return trueCondition();
        // 공개 조건: 방송중, 예약, 준비중 OR (VOD이면서 공개상태)
        return broadcastStatus.in(
                        BroadcastStatus.ON_AIR.name(),
                        BroadcastStatus.READY.name(),
                        BroadcastStatus.RESERVED.name()
                )
                .or(vodStatus.eq(VodStatus.PUBLIC.name()));
    }

    // [상세 필터] VOD 공개/비공개 선택
    private Condition publicFilter(Boolean isPublic) {
        if (isPublic == null) return trueCondition();
        return isPublic ? vodStatus.eq(VodStatus.PUBLIC.name()) : vodStatus.in(VodStatus.PRIVATE.name(), VodStatus.DELETED.name());
    }

    // [상세 필터] 예약중/취소됨 등 상태 직접 필터링
    private Condition statusDetailFilter(String status) {
        if (status == null || "ALL".equalsIgnoreCase(status)) return trueCondition();
        try {
            return broadcastStatus.eq(BroadcastStatus.valueOf(status).name());
        } catch (Exception e) {
            return trueCondition(); // 유효하지 않은 상태값 무시
        }
    }

    private Condition tabCondition(String tab) {
        if (tab == null || "ALL".equalsIgnoreCase(tab)) return trueCondition();
        if ("LIVE".equalsIgnoreCase(tab)) return broadcastStatus.in(BroadcastStatus.ON_AIR.name(), BroadcastStatus.READY.name());
        if ("RESERVED".equalsIgnoreCase(tab)) return broadcastStatus.in(BroadcastStatus.RESERVED.name(), BroadcastStatus.CANCELED.name());
        if ("VOD".equalsIgnoreCase(tab)) return broadcastStatus.in(BroadcastStatus.VOD.name(), BroadcastStatus.ENDED.name(), BroadcastStatus.STOPPED.name());
        return trueCondition();
    }

    // [정렬 로직]
    private SortField<?> getOrderSpecifier(BroadcastSearch condition, Field<Long> reportCount) {
        String sort = condition.getSortType();
        String tab = condition.getTab();

        // 1. 관리자 신고순
        if ("REPORT".equalsIgnoreCase(sort)) return reportCount.desc();
        // 2. 판매자 매출순
        if ("SALES".equalsIgnoreCase(sort)) return totalSales.desc();

        // 3. 인기순 (VOD: DB 조회수, LIVE: 시작시간 -> Redis는 메모리 정렬)
        if ("POPULAR".equalsIgnoreCase(sort) || "VIEWER".equalsIgnoreCase(sort)) {
            if ("VOD".equalsIgnoreCase(tab)) return totalViews.desc();
            return startedAt.desc().nullsLast();
        }

        // 4. 좋아요순
        if ("LIKE_DESC".equalsIgnoreCase(sort)) return totalLikes.desc();
        if ("LIKE_ASC".equalsIgnoreCase(sort)) return totalLikes.asc();

        // 5. 시작 임박순
        if ("RESERVED".equalsIgnoreCase(tab) || "START_ASC".equalsIgnoreCase(sort)) {
            return scheduledAt.asc().nullsLast();
        }

        // 기본값: 최신순 (예약일 기준)
        return scheduledAt.desc().nullsLast();
    }

    private SortField<?> getOrderSpecifier(BroadcastSortOrder sortOrder, Field<Long> reportCount) {
        return switch (sortOrder) {
            case STARTED_AT_DESC -> startedAt.desc().nullsLast();
            case SCHEDULED_AT_ASC -> scheduledAt.asc().nullsLast();
            case ENDED_AT_DESC -> endedAt.desc().nullsLast();
        };
    }

    private BroadcastListResponse mapBroadcastList(Record record) {
        return new BroadcastListResponse(
                record.get(broadcastId),
                record.get(broadcastTitle),
                record.get(broadcastNotice),
                record.get(sellerName),
                record.get(tagCategoryName),
                record.get(broadcastThumbUrl),
                BroadcastStatus.valueOf(record.get(broadcastStatus)),
                record.get(scheduledAt),
                record.get(startedAt),
                record.get(endedAt),
                record.get(totalViews),
                record.get(vodStatus) != null ? VodStatus.valueOf(record.get(vodStatus)) : null,
                record.get("report_count", Long.class),
                record.get(totalSales),
                record.get(totalLikes)
        );
    }
}
