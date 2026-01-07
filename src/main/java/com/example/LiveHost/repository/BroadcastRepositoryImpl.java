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
import java.util.List;

@RequiredArgsConstructor
public class BroadcastRepositoryImpl implements BroadcastRepositoryCustom {

    private static final Table<?> BROADCAST = DSL.table(DSL.name("broadcast"));
    private static final Table<?> SELLER = DSL.table(DSL.name("seller"));
    private static final Table<?> TAG_CATEGORY = DSL.table(DSL.name("tag_category"));
    private static final Table<?> BROADCAST_RESULT = DSL.table(DSL.name("broadcast_result"));
    private static final Table<?> VOD = DSL.table(DSL.name("vod"));
    private static final Table<?> SANCTION = DSL.table(DSL.name("sanction"));

    private static final Field<Long> BROADCAST_ID = DSL.field(DSL.name("broadcast", "broadcast_id"), Long.class);
    private static final Field<String> BROADCAST_TITLE = DSL.field(DSL.name("broadcast", "broadcast_title"), String.class);
    private static final Field<String> BROADCAST_NOTICE = DSL.field(DSL.name("broadcast", "broadcast_notice"), String.class);
    private static final Field<String> BROADCAST_STATUS = DSL.field(DSL.name("broadcast", "status"), String.class);
    private static final Field<LocalDateTime> BROADCAST_SCHEDULED_AT = DSL.field(DSL.name("broadcast", "scheduled_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> BROADCAST_STARTED_AT = DSL.field(DSL.name("broadcast", "started_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> BROADCAST_ENDED_AT = DSL.field(DSL.name("broadcast", "ended_at"), LocalDateTime.class);
    private static final Field<String> BROADCAST_THUMB_URL = DSL.field(DSL.name("broadcast", "broadcast_thumb_url"), String.class);
    private static final Field<Long> BROADCAST_SELLER_ID = DSL.field(DSL.name("broadcast", "seller_id"), Long.class);
    private static final Field<Long> BROADCAST_TAG_CATEGORY_ID = DSL.field(DSL.name("broadcast", "tag_category_id"), Long.class);

    private static final Field<Long> SELLER_ID = DSL.field(DSL.name("seller", "seller_id"), Long.class);
    private static final Field<String> SELLER_NAME = DSL.field(DSL.name("seller", "name"), String.class);

    private static final Field<Long> TAG_CATEGORY_ID = DSL.field(DSL.name("tag_category", "tag_category_id"), Long.class);
    private static final Field<String> TAG_CATEGORY_NAME = DSL.field(DSL.name("tag_category", "tag_category_name"), String.class);

    private static final Field<Long> BROADCAST_RESULT_BROADCAST_ID = DSL.field(DSL.name("broadcast_result", "broadcast_id"), Long.class);
    private static final Field<Integer> BROADCAST_RESULT_VIEWS = DSL.field(DSL.name("broadcast_result", "total_views"), Integer.class);
    private static final Field<Integer> BROADCAST_RESULT_LIKES = DSL.field(DSL.name("broadcast_result", "total_likes"), Integer.class);
    private static final Field<java.math.BigDecimal> BROADCAST_RESULT_SALES = DSL.field(DSL.name("broadcast_result", "total_sales"), java.math.BigDecimal.class);

    private static final Field<Long> VOD_BROADCAST_ID = DSL.field(DSL.name("vod", "broadcast_id"), Long.class);
    private static final Field<String> VOD_STATUS = DSL.field(DSL.name("vod", "status"), String.class);

    private static final Field<Long> SANCTION_ID = DSL.field(DSL.name("sanction", "sanction_id"), Long.class);
    private static final Field<Long> SANCTION_BROADCAST_ID = DSL.field(DSL.name("sanction", "broadcast_id"), Long.class);

    private final DSLContext dsl;

    @Override
    public Slice<BroadcastListResponse> searchBroadcasts(Long sellerId, BroadcastSearch condition, Pageable pageable, boolean isAdmin) {
        Field<Long> reportCountField = DSL.count(SANCTION_ID).cast(Long.class).as("report_count");
        Condition filters = DSL.trueCondition()
                .and(sellerIdEq(sellerId))
                .and(tabCondition(condition.getTab()))
                .and(keywordContains(condition.getKeyword()))
                .and(categoryEq(condition.getCategoryId()))
                .and(dateBetween(condition.getStartDate(), condition.getEndDate()))
                .and(statusDetailFilter(condition.getStatusFilter()))
                .and(publicFilter(condition.getIsPublic()))
                .and(publicCondition(isAdmin))
                .and(BROADCAST_STATUS.ne(BroadcastStatus.DELETED.name()));

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
                        BROADCAST_RESULT_VIEWS,
                        VOD_STATUS,
                        reportCountField,
                        BROADCAST_RESULT_SALES,
                        BROADCAST_RESULT_LIKES
                )
                .from(BROADCAST)
                .join(SELLER).on(BROADCAST_SELLER_ID.eq(SELLER_ID))
                .join(TAG_CATEGORY).on(BROADCAST_TAG_CATEGORY_ID.eq(TAG_CATEGORY_ID))
                .leftJoin(BROADCAST_RESULT).on(BROADCAST_ID.eq(BROADCAST_RESULT_BROADCAST_ID))
                .leftJoin(VOD).on(BROADCAST_ID.eq(VOD_BROADCAST_ID))
                .leftJoin(SANCTION).on(SANCTION_BROADCAST_ID.eq(BROADCAST_ID))
                .where(filters)
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
                        BROADCAST_RESULT_VIEWS,
                        VOD_STATUS,
                        BROADCAST_RESULT_SALES,
                        BROADCAST_RESULT_LIKES
                )
                .orderBy(getOrderField(condition, reportCountField))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch(this::toBroadcastListResponse);

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }
        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public List<BroadcastListResponse> findTop5ByStatus(Long sellerId, List<BroadcastStatus> statuses, String orderByField, boolean desc, boolean isAdmin) {
        Field<Long> reportCountField = DSL.inline(0L).as("report_count");
        Condition filters = DSL.trueCondition()
                .and(sellerIdEq(sellerId))
                .and(BROADCAST_STATUS.in(statuses.stream().map(Enum::name).toList()))
                .and(publicCondition(isAdmin))
                .and(BROADCAST_STATUS.ne(BroadcastStatus.DELETED.name()));

        Field<LocalDateTime> orderField = resolveOrderField(orderByField);
        SortField<?> sortField = desc ? orderField.desc() : orderField.asc();

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
                        BROADCAST_RESULT_VIEWS,
                        VOD_STATUS,
                        reportCountField,
                        BROADCAST_RESULT_SALES,
                        BROADCAST_RESULT_LIKES
                )
                .from(BROADCAST)
                .join(SELLER).on(BROADCAST_SELLER_ID.eq(SELLER_ID))
                .join(TAG_CATEGORY).on(BROADCAST_TAG_CATEGORY_ID.eq(TAG_CATEGORY_ID))
                .leftJoin(BROADCAST_RESULT).on(BROADCAST_ID.eq(BROADCAST_RESULT_BROADCAST_ID))
                .leftJoin(VOD).on(BROADCAST_ID.eq(VOD_BROADCAST_ID))
                .where(filters)
                .orderBy(sortField)
                .limit(5)
                .fetch(this::toBroadcastListResponse);
    }

    @Override
    public long countByTimeSlot(LocalDateTime start, LocalDateTime end) {
        Integer count = dsl
                .selectCount()
                .from(BROADCAST)
                .where(
                        BROADCAST_STATUS.eq(BroadcastStatus.RESERVED.name()),
                        BROADCAST_SCHEDULED_AT.between(start, end),
                        BROADCAST_STATUS.ne(BroadcastStatus.DELETED.name())
                )
                .fetchOne(0, Integer.class);
        return count != null ? count : 0;
    }

    private BroadcastListResponse toBroadcastListResponse(Record record) {
        return new BroadcastListResponse(
                record.get(BROADCAST_ID),
                record.get(BROADCAST_TITLE),
                record.get(BROADCAST_NOTICE),
                record.get(SELLER_NAME),
                record.get(TAG_CATEGORY_NAME),
                record.get(BROADCAST_THUMB_URL),
                toBroadcastStatus(record.get(BROADCAST_STATUS)),
                record.get(BROADCAST_SCHEDULED_AT),
                record.get(BROADCAST_STARTED_AT),
                record.get(BROADCAST_ENDED_AT),
                record.get(BROADCAST_RESULT_VIEWS),
                toVodStatus(record.get(VOD_STATUS)),
                record.get("report_count", Long.class),
                record.get(BROADCAST_RESULT_SALES),
                record.get(BROADCAST_RESULT_LIKES)
        );
    }

    private BroadcastStatus toBroadcastStatus(String status) {
        return status != null ? BroadcastStatus.valueOf(status) : null;
    }

    private VodStatus toVodStatus(String status) {
        return status != null ? VodStatus.valueOf(status) : null;
    }

    private Condition sellerIdEq(Long sellerId) {
        return sellerId != null ? BROADCAST_SELLER_ID.eq(sellerId) : DSL.trueCondition();
    }

    private Condition categoryEq(Long categoryId) {
        return categoryId != null ? BROADCAST_TAG_CATEGORY_ID.eq(categoryId) : DSL.trueCondition();
    }

    private Condition keywordContains(String keyword) {
        return (keyword != null && !keyword.isEmpty()) ? BROADCAST_TITLE.like("%" + keyword + "%") : DSL.trueCondition();
    }

    private Condition dateBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return DSL.trueCondition();
        }
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.atTime(23, 59, 59);
        return BROADCAST_STARTED_AT.between(startTime, endTime)
                .or(BROADCAST_SCHEDULED_AT.between(startTime, endTime));
    }

    private Condition publicCondition(boolean isAdmin) {
        if (isAdmin) {
            return DSL.trueCondition();
        }
        return BROADCAST_STATUS.in(
                BroadcastStatus.ON_AIR.name(),
                BroadcastStatus.READY.name(),
                BroadcastStatus.RESERVED.name()
        ).or(VOD_STATUS.eq(VodStatus.PUBLIC.name()));
    }

    private Condition publicFilter(Boolean isPublic) {
        if (isPublic == null) {
            return DSL.trueCondition();
        }
        if (isPublic) {
            return VOD_STATUS.eq(VodStatus.PUBLIC.name());
        }
        return VOD_STATUS.in(VodStatus.PRIVATE.name(), VodStatus.DELETED.name());
    }

    private Condition statusDetailFilter(String status) {
        if (status == null || "ALL".equalsIgnoreCase(status)) {
            return DSL.trueCondition();
        }
        try {
            return BROADCAST_STATUS.eq(BroadcastStatus.valueOf(status).name());
        } catch (IllegalArgumentException e) {
            return DSL.trueCondition();
        }
    }

    private Condition tabCondition(String tab) {
        if (tab == null || "ALL".equalsIgnoreCase(tab)) {
            return DSL.trueCondition();
        }
        if ("LIVE".equalsIgnoreCase(tab)) {
            return BROADCAST_STATUS.in(BroadcastStatus.ON_AIR.name(), BroadcastStatus.READY.name());
        }
        if ("RESERVED".equalsIgnoreCase(tab)) {
            return BROADCAST_STATUS.in(BroadcastStatus.RESERVED.name(), BroadcastStatus.CANCELED.name());
        }
        if ("VOD".equalsIgnoreCase(tab)) {
            return BROADCAST_STATUS.in(
                    BroadcastStatus.VOD.name(),
                    BroadcastStatus.ENDED.name(),
                    BroadcastStatus.STOPPED.name()
            );
        }
        return DSL.trueCondition();
    }

    private SortField<?> getOrderField(BroadcastSearch condition, Field<Long> reportCountField) {
        String sort = condition.getSortType();
        String tab = condition.getTab();

        if ("REPORT".equalsIgnoreCase(sort)) {
            return reportCountField.desc();
        }
        if ("SALES".equalsIgnoreCase(sort)) {
            return BROADCAST_RESULT_SALES.desc();
        }
        if ("POPULAR".equalsIgnoreCase(sort) || "VIEWER".equalsIgnoreCase(sort)) {
            if ("VOD".equalsIgnoreCase(tab)) {
                return BROADCAST_RESULT_VIEWS.desc();
            }
            return BROADCAST_STARTED_AT.desc();
        }
        if ("LIKE_DESC".equalsIgnoreCase(sort)) {
            return BROADCAST_RESULT_LIKES.desc();
        }
        if ("LIKE_ASC".equalsIgnoreCase(sort)) {
            return BROADCAST_RESULT_LIKES.asc();
        }
        if ("RESERVED".equalsIgnoreCase(tab) || "START_ASC".equalsIgnoreCase(sort)) {
            return BROADCAST_SCHEDULED_AT.asc();
        }
        return BROADCAST_SCHEDULED_AT.desc();
    }

    private Field<LocalDateTime> resolveOrderField(String orderByField) {
        if ("started_at".equalsIgnoreCase(orderByField)) {
            return BROADCAST_STARTED_AT;
        }
        if ("ended_at".equalsIgnoreCase(orderByField)) {
            return BROADCAST_ENDED_AT;
        }
        return BROADCAST_SCHEDULED_AT;
    }
}
