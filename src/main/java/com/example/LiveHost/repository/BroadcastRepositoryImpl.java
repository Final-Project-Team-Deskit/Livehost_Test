package com.example.LiveHost.repository;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.enums.VodStatus;
import com.example.LiveHost.dto.request.BroadcastSearch;
import com.example.LiveHost.dto.response.BroadcastListResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class BroadcastRepositoryImpl implements BroadcastRepositoryCustom {

    private static final Table<Record> BROADCAST = DSL.table(DSL.name("broadcast")).as("b");
    private static final Table<Record> SELLER = DSL.table(DSL.name("seller")).as("s");
    private static final Table<Record> TAG_CATEGORY = DSL.table(DSL.name("tag_category")).as("tc");
    private static final Table<Record> BROADCAST_RESULT = DSL.table(DSL.name("broadcast_result")).as("br");
    private static final Table<Record> VOD = DSL.table(DSL.name("vod")).as("v");
    private static final Table<Record> SANCTION = DSL.table(DSL.name("sanction")).as("sc");

    private static final Field<Long> BROADCAST_ID = DSL.field(DSL.name("b", "broadcast_id"), Long.class);
    private static final Field<String> BROADCAST_TITLE = DSL.field(DSL.name("b", "broadcast_title"), String.class);
    private static final Field<String> BROADCAST_NOTICE = DSL.field(DSL.name("b", "broadcast_notice"), String.class);
    private static final Field<String> BROADCAST_THUMB_URL = DSL.field(DSL.name("b", "broadcast_thumb_url"), String.class);
    private static final Field<BroadcastStatus> BROADCAST_STATUS = DSL.field(DSL.name("b", "status"), BroadcastStatus.class);
    private static final Field<LocalDateTime> BROADCAST_SCHEDULED_AT = DSL.field(DSL.name("b", "scheduled_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> BROADCAST_STARTED_AT = DSL.field(DSL.name("b", "started_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> BROADCAST_ENDED_AT = DSL.field(DSL.name("b", "ended_at"), LocalDateTime.class);
    private static final Field<Long> BROADCAST_SELLER_ID = DSL.field(DSL.name("b", "seller_id"), Long.class);
    private static final Field<Long> BROADCAST_CATEGORY_ID = DSL.field(DSL.name("b", "tag_category_id"), Long.class);

    private static final Field<String> SELLER_NAME = DSL.field(DSL.name("s", "name"), String.class);

    private static final Field<String> TAG_CATEGORY_NAME = DSL.field(DSL.name("tc", "tag_category_name"), String.class);

    private static final Field<Long> BROADCAST_RESULT_BROADCAST_ID = DSL.field(DSL.name("br", "broadcast_id"), Long.class);
    private static final Field<Integer> BROADCAST_RESULT_TOTAL_VIEWS = DSL.field(DSL.name("br", "total_views"), Integer.class);
    private static final Field<java.math.BigDecimal> BROADCAST_RESULT_TOTAL_SALES = DSL.field(DSL.name("br", "total_sales"), java.math.BigDecimal.class);
    private static final Field<Integer> BROADCAST_RESULT_TOTAL_LIKES = DSL.field(DSL.name("br", "total_likes"), Integer.class);

    private static final Field<Long> VOD_BROADCAST_ID = DSL.field(DSL.name("v", "broadcast_id"), Long.class);
    private static final Field<VodStatus> VOD_STATUS = DSL.field(DSL.name("v", "status"), VodStatus.class);

    private static final Field<Long> SANCTION_ID = DSL.field(DSL.name("sc", "sanction_id"), Long.class);
    private static final Field<Long> SANCTION_BROADCAST_ID = DSL.field(DSL.name("sc", "broadcast_id"), Long.class);

    private final DSLContext dsl;

    @Override
    public Slice<BroadcastListResponse> searchBroadcasts(Long sellerId, BroadcastSearch condition, Pageable pageable, boolean isAdmin) {
        List<Condition> conditions = buildSearchConditions(sellerId, condition, isAdmin);
        Field<Long> reportCount = DSL.count(SANCTION_ID).as("report_count");

        List<BroadcastListResponse> content = dsl
                .select(
                        BROADCAST_ID,
                        BROADCAST_TITLE,
                        BROADCAST_NOTICE,
                        SELLER_NAME,
                        TAG_CATEGORY_NAME,
                        BROADCAST_THUMB_URL,
                        BROADCAST_STATUS,
                        BROADCAST_SCHEDULED_AT,
                        BROADCAST_STARTED_AT,
                        BROADCAST_ENDED_AT,
                        BROADCAST_RESULT_TOTAL_VIEWS,
                        VOD_STATUS,
                        reportCount,
                        BROADCAST_RESULT_TOTAL_SALES,
                        BROADCAST_RESULT_TOTAL_LIKES
                )
                .from(BROADCAST)
                .join(SELLER).on(BROADCAST_SELLER_ID.eq(DSL.field(DSL.name("s", "seller_id"), Long.class)))
                .join(TAG_CATEGORY).on(BROADCAST_CATEGORY_ID.eq(DSL.field(DSL.name("tc", "tag_category_id"), Long.class)))
                .leftJoin(BROADCAST_RESULT).on(BROADCAST_ID.eq(BROADCAST_RESULT_BROADCAST_ID))
                .leftJoin(VOD).on(BROADCAST_ID.eq(VOD_BROADCAST_ID))
                .leftJoin(SANCTION).on(SANCTION_BROADCAST_ID.eq(BROADCAST_ID))
                .where(conditions)
                .groupBy(
                        BROADCAST_ID,
                        BROADCAST_TITLE,
                        BROADCAST_NOTICE,
                        SELLER_NAME,
                        TAG_CATEGORY_NAME,
                        BROADCAST_THUMB_URL,
                        BROADCAST_STATUS,
                        BROADCAST_SCHEDULED_AT,
                        BROADCAST_STARTED_AT,
                        BROADCAST_ENDED_AT,
                        BROADCAST_RESULT_TOTAL_VIEWS,
                        VOD_STATUS,
                        BROADCAST_RESULT_TOTAL_SALES,
                        BROADCAST_RESULT_TOTAL_LIKES
                )
                .orderBy(getOrderSpecifier(condition, reportCount))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch(record -> mapToBroadcastListResponse(record, reportCount));

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }
        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public List<BroadcastListResponse> findTop5ByStatus(Long sellerId, List<BroadcastStatus> statuses, BroadcastOrderField orderField, boolean isAsc, boolean isAdmin) {
        List<Condition> conditions = new ArrayList<>();
        if (sellerId != null) {
            conditions.add(BROADCAST_SELLER_ID.eq(sellerId));
        }
        conditions.add(BROADCAST_STATUS.in(statuses));
        Condition publicCondition = publicCondition(isAdmin);
        if (publicCondition != null) {
            conditions.add(publicCondition);
        }
        conditions.add(BROADCAST_STATUS.ne(BroadcastStatus.DELETED));

        Field<Long> reportCount = DSL.inline(0L).as("report_count");

        return dsl
                .select(
                        BROADCAST_ID,
                        BROADCAST_TITLE,
                        BROADCAST_NOTICE,
                        SELLER_NAME,
                        TAG_CATEGORY_NAME,
                        BROADCAST_THUMB_URL,
                        BROADCAST_STATUS,
                        BROADCAST_SCHEDULED_AT,
                        BROADCAST_STARTED_AT,
                        BROADCAST_ENDED_AT,
                        BROADCAST_RESULT_TOTAL_VIEWS,
                        VOD_STATUS,
                        reportCount,
                        BROADCAST_RESULT_TOTAL_SALES,
                        BROADCAST_RESULT_TOTAL_LIKES
                )
                .from(BROADCAST)
                .join(SELLER).on(BROADCAST_SELLER_ID.eq(DSL.field(DSL.name("s", "seller_id"), Long.class)))
                .join(TAG_CATEGORY).on(BROADCAST_CATEGORY_ID.eq(DSL.field(DSL.name("tc", "tag_category_id"), Long.class)))
                .leftJoin(BROADCAST_RESULT).on(BROADCAST_ID.eq(BROADCAST_RESULT_BROADCAST_ID))
                .leftJoin(VOD).on(BROADCAST_ID.eq(VOD_BROADCAST_ID))
                .where(conditions)
                .orderBy(getOverviewOrderSpecifier(orderField, isAsc))
                .limit(5)
                .fetch(record -> mapToBroadcastListResponse(record, reportCount));
    }

    @Override
    public long countByTimeSlot(LocalDateTime start, LocalDateTime end) {
        Long count = dsl
                .selectCount()
                .from(BROADCAST)
                .where(
                        BROADCAST_STATUS.eq(BroadcastStatus.RESERVED),
                        BROADCAST_SCHEDULED_AT.between(start, end),
                        BROADCAST_STATUS.ne(BroadcastStatus.DELETED)
                )
                .fetchOne(0, Long.class);
        return count != null ? count : 0;
    }

    @Override
    public Map<LocalDateTime, Long> countReservedByScheduledAtBetween(LocalDateTime start, LocalDateTime end) {
        Field<Long> countField = DSL.count().as("slot_count");
        List<Record> records = dsl
                .select(BROADCAST_SCHEDULED_AT, countField)
                .from(BROADCAST)
                .where(
                        BROADCAST_STATUS.eq(BroadcastStatus.RESERVED),
                        BROADCAST_SCHEDULED_AT.between(start, end),
                        BROADCAST_STATUS.ne(BroadcastStatus.DELETED)
                )
                .groupBy(BROADCAST_SCHEDULED_AT)
                .fetch();

        Map<LocalDateTime, Long> result = new HashMap<>();
        for (Record record : records) {
            LocalDateTime scheduledAt = record.get(BROADCAST_SCHEDULED_AT);
            Long count = record.get(countField);
            if (scheduledAt != null && count != null) {
                result.put(scheduledAt, count);
            }
        }
        return result;
    }

    private List<Condition> buildSearchConditions(Long sellerId, BroadcastSearch condition, boolean isAdmin) {
        List<Condition> conditions = new ArrayList<>();
        Condition sellerCondition = sellerIdEq(sellerId);
        if (sellerCondition != null) {
            conditions.add(sellerCondition);
        }
        Condition tabCondition = tabCondition(condition.getTab());
        if (tabCondition != null) {
            conditions.add(tabCondition);
        }
        Condition keywordCondition = keywordContains(condition.getKeyword());
        if (keywordCondition != null) {
            conditions.add(keywordCondition);
        }
        Condition categoryCondition = categoryEq(condition.getCategoryId());
        if (categoryCondition != null) {
            conditions.add(categoryCondition);
        }
        Condition dateCondition = dateBetween(condition.getStartDate(), condition.getEndDate());
        if (dateCondition != null) {
            conditions.add(dateCondition);
        }
        Condition statusCondition = statusDetailFilter(condition.getStatusFilter());
        if (statusCondition != null) {
            conditions.add(statusCondition);
        }
        Condition publicFilter = publicFilter(condition.getIsPublic());
        if (publicFilter != null) {
            conditions.add(publicFilter);
        }
        Condition publicCondition = publicCondition(isAdmin);
        if (publicCondition != null) {
            conditions.add(publicCondition);
        }
        conditions.add(BROADCAST_STATUS.ne(BroadcastStatus.DELETED));
        return conditions;
    }

    private BroadcastListResponse mapToBroadcastListResponse(Record record, Field<Long> reportCountField) {
        return new BroadcastListResponse(
                record.get(BROADCAST_ID),
                record.get(BROADCAST_TITLE),
                record.get(BROADCAST_NOTICE),
                record.get(SELLER_NAME),
                record.get(TAG_CATEGORY_NAME),
                record.get(BROADCAST_THUMB_URL),
                record.get(BROADCAST_STATUS),
                record.get(BROADCAST_SCHEDULED_AT),
                record.get(BROADCAST_STARTED_AT),
                record.get(BROADCAST_ENDED_AT),
                record.get(BROADCAST_RESULT_TOTAL_VIEWS),
                record.get(VOD_STATUS),
                record.get(reportCountField),
                record.get(BROADCAST_RESULT_TOTAL_SALES),
                record.get(BROADCAST_RESULT_TOTAL_LIKES)
        );
    }

    private Condition sellerIdEq(Long sellerId) {
        return sellerId != null ? BROADCAST_SELLER_ID.eq(sellerId) : null;
    }

    private Condition categoryEq(Long categoryId) {
        return categoryId != null ? BROADCAST_CATEGORY_ID.eq(categoryId) : null;
    }

    private Condition keywordContains(String keyword) {
        return (keyword != null && !keyword.isEmpty()) ? BROADCAST_TITLE.contains(keyword) : null;
    }

    private Condition dateBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return null;
        }
        return BROADCAST_STARTED_AT.between(start.atStartOfDay(), end.atTime(23, 59, 59))
                .or(BROADCAST_SCHEDULED_AT.between(start.atStartOfDay(), end.atTime(23, 59, 59)));
    }

    private Condition publicCondition(boolean isAdmin) {
        if (isAdmin) {
            return null;
        }
        return BROADCAST_STATUS.in(BroadcastStatus.ON_AIR, BroadcastStatus.READY, BroadcastStatus.RESERVED)
                .or(VOD_STATUS.eq(VodStatus.PUBLIC));
    }

    private Condition publicFilter(Boolean isPublic) {
        if (isPublic == null) {
            return null;
        }
        return isPublic ? VOD_STATUS.eq(VodStatus.PUBLIC) : VOD_STATUS.in(VodStatus.PRIVATE, VodStatus.DELETED);
    }

    private Condition statusDetailFilter(String status) {
        if (status == null || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        try {
            return BROADCAST_STATUS.eq(BroadcastStatus.valueOf(status));
        } catch (Exception e) {
            return null;
        }
    }

    private Condition tabCondition(String tab) {
        if (tab == null || "ALL".equalsIgnoreCase(tab)) {
            return null;
        }
        if ("LIVE".equalsIgnoreCase(tab)) {
            return BROADCAST_STATUS.in(BroadcastStatus.ON_AIR, BroadcastStatus.READY);
        }
        if ("RESERVED".equalsIgnoreCase(tab)) {
            return BROADCAST_STATUS.in(BroadcastStatus.RESERVED, BroadcastStatus.CANCELED);
        }
        if ("VOD".equalsIgnoreCase(tab)) {
            return BROADCAST_STATUS.in(BroadcastStatus.VOD, BroadcastStatus.ENDED, BroadcastStatus.STOPPED);
        }
        return null;
    }

    private SortField<?> getOrderSpecifier(BroadcastSearch condition, Field<Long> reportCount) {
        String sort = condition.getSortType();
        String tab = condition.getTab();

        if ("REPORT".equalsIgnoreCase(sort)) {
            return reportCount.desc();
        }
        if ("SALES".equalsIgnoreCase(sort)) {
            return BROADCAST_RESULT_TOTAL_SALES.desc().nullsLast();
        }
        if ("POPULAR".equalsIgnoreCase(sort) || "VIEWER".equalsIgnoreCase(sort)) {
            if ("VOD".equalsIgnoreCase(tab)) {
                return BROADCAST_RESULT_TOTAL_VIEWS.desc().nullsLast();
            }
            return BROADCAST_STARTED_AT.desc().nullsLast();
        }
        if ("LIKE_DESC".equalsIgnoreCase(sort)) {
            return BROADCAST_RESULT_TOTAL_LIKES.desc().nullsLast();
        }
        if ("LIKE_ASC".equalsIgnoreCase(sort)) {
            return BROADCAST_RESULT_TOTAL_LIKES.asc().nullsLast();
        }
        if ("RESERVED".equalsIgnoreCase(tab) || "START_ASC".equalsIgnoreCase(sort)) {
            return BROADCAST_SCHEDULED_AT.asc().nullsLast();
        }
        return BROADCAST_SCHEDULED_AT.desc().nullsLast();
    }

    private SortField<?> getOverviewOrderSpecifier(BroadcastOrderField orderField, boolean isAsc) {
        Field<LocalDateTime> targetField = switch (orderField) {
            case STARTED_AT -> BROADCAST_STARTED_AT;
            case SCHEDULED_AT -> BROADCAST_SCHEDULED_AT;
            case ENDED_AT -> BROADCAST_ENDED_AT;
        };
        return isAsc ? targetField.asc().nullsLast() : targetField.desc().nullsLast();
    }
}
