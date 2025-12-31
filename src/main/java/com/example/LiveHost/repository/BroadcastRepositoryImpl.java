package com.example.LiveHost.repository;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.enums.VodStatus;
import com.example.LiveHost.dto.BroadcastListResponse;
import com.example.LiveHost.dto.BroadcastSearch;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.LiveHost.entity.QBroadcast.broadcast;
import static com.example.LiveHost.entity.QBroadcastResult.broadcastResult;
import static com.example.LiveHost.others.entity.QSeller.seller;
import static com.example.LiveHost.others.entity.QTagCategory.tagCategory;
import static com.example.LiveHost.entity.QVod.vod;
import static com.example.LiveHost.entity.QSanction.sanction;

@RequiredArgsConstructor
public class BroadcastRepositoryImpl implements BroadcastRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<BroadcastListResponse> searchBroadcasts(Long sellerId, BroadcastSearch condition, Pageable pageable, boolean isAdmin) {
        boolean isReportSort = "REPORT".equalsIgnoreCase(condition.getSortType());

        List<BroadcastListResponse> content = queryFactory
                .select(Projections.constructor(BroadcastListResponse.class,
                        broadcast.broadcastId, broadcast.broadcastTitle, broadcast.broadcastNotice,
                        seller.name, tagCategory.tagCategoryName, broadcast.broadcastThumbUrl,
                        broadcast.status, broadcast.scheduledAt, broadcast.startedAt, broadcast.endedAt,
                        broadcastResult.totalViews,  // DB 통계
                        vod.status,                  // VOD 상태
                        sanction.count(),            // 신고 수
                        broadcastResult.totalSales,  // 매출
                        broadcastResult.totalLikes   // 좋아요
                ))
                .from(broadcast)
                .join(broadcast.seller, seller)
                .join(broadcast.tagCategory, tagCategory)
                .leftJoin(broadcastResult).on(broadcast.broadcastId.eq(broadcastResult.broadcast.broadcastId))
                .leftJoin(vod).on(broadcast.broadcastId.eq(vod.broadcast.broadcastId))
                .leftJoin(sanction).on(isReportSort ? sanction.broadcast.broadcastId.eq(broadcast.broadcastId) : null)
                .where(
                        sellerIdEq(sellerId),
                        tabCondition(condition.getTab()),
                        keywordContains(condition.getKeyword()),
                        categoryEq(condition.getCategoryId()),
                        // [수정] DTO에 있는 날짜 필드 사용
                        dateBetween(condition.getStartDate(), condition.getEndDate()),
                        statusDetailFilter(condition.getStatusFilter()),
                        publicFilter(condition.getIsPublic()),
                        publicCondition(isAdmin),
                        broadcast.status.ne(BroadcastStatus.DELETED)
                )
                .groupBy(broadcast.broadcastId)
                .orderBy(getOrderSpecifier(condition))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }
        return new SliceImpl<>(content, pageable, hasNext);
    }

    // 2. Overview 조회 (Tab=ALL, Top 5)
    @Override
    public List<BroadcastListResponse> findTop5ByStatus(Long sellerId, List<BroadcastStatus> statuses, OrderSpecifier<?> orderSpecifier, boolean isAdmin) {
        return queryFactory
                .select(Projections.constructor(BroadcastListResponse.class,
                        broadcast.broadcastId, broadcast.broadcastTitle, broadcast.broadcastNotice,
                        seller.name, tagCategory.tagCategoryName, broadcast.broadcastThumbUrl,
                        broadcast.status, broadcast.scheduledAt, broadcast.startedAt, broadcast.endedAt,
                        broadcastResult.totalViews, vod.status,
                        com.querydsl.core.types.dsl.Expressions.constant(0L), // Report Count
                        broadcastResult.totalSales,
                        broadcastResult.totalLikes // [추가] DB 좋아요 수
                ))
                .from(broadcast)
                .join(broadcast.seller, seller)
                .join(broadcast.tagCategory, tagCategory)
                .leftJoin(broadcastResult).on(broadcast.broadcastId.eq(broadcastResult.broadcastId))
                .leftJoin(vod).on(broadcast.broadcastId.eq(vod.broadcast.broadcastId))
                .where(
                        // ... (기존 조건들 유지) ...
                        sellerIdEq(sellerId),
                        broadcast.status.in(statuses),
                        publicCondition(isAdmin),
                        broadcast.status.ne(BroadcastStatus.DELETED)
                )
                .orderBy(orderSpecifier)
                .limit(5)
                .fetch();
    }

    // 3. 시간 슬롯 예약 확인 (플랫폼 전체 3개 제한)
    @Override
    public long countByTimeSlot(LocalDateTime start, LocalDateTime end) {
        Long count = queryFactory
                .select(broadcast.count())
                .from(broadcast)
                .where(
                        broadcast.status.eq(BroadcastStatus.RESERVED),
                        broadcast.scheduledAt.between(start, end),
                        broadcast.status.ne(BroadcastStatus.DELETED)
                )
                .fetchOne();
        return count != null ? count : 0;
    }

    // --- Helper Methods (누락된 메서드 구현) ---

    private BooleanExpression sellerIdEq(Long sellerId) {
        return sellerId != null ? broadcast.seller.sellerId.eq(sellerId) : null;
    }

    private BooleanExpression categoryEq(Long categoryId) {
        return categoryId != null ? broadcast.tagCategory.tagCategoryId.eq(categoryId) : null;
    }

    private BooleanExpression keywordContains(String k) {
        return (k != null && !k.isEmpty()) ? broadcast.broadcastTitle.contains(k) : null;
    }

    // [날짜 필터] 시작일~종료일 사이에 방송이 시작되었거나 예정된 경우
    private BooleanExpression dateBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) return null;
        return broadcast.startedAt.between(start.atStartOfDay(), end.atTime(23, 59, 59))
                .or(broadcast.scheduledAt.between(start.atStartOfDay(), end.atTime(23, 59, 59)));
    }

    // [권한 필터] 관리자/판매자는 모두 조회, 일반 유저는 공개된 방송만 조회
    private BooleanExpression publicCondition(boolean isAdmin) {
        if (isAdmin) return null;
        // 공개 조건: 방송중, 예약, 준비중 OR (VOD이면서 공개상태)
        return broadcast.status.in(BroadcastStatus.ON_AIR, BroadcastStatus.READY, BroadcastStatus.RESERVED)
                .or(vod.status.eq(VodStatus.PUBLIC));
    }

    // [상세 필터] VOD 공개/비공개 선택
    private BooleanExpression publicFilter(Boolean isPublic) {
        if (isPublic == null) return null;
        return isPublic ? vod.status.eq(VodStatus.PUBLIC) : vod.status.in(VodStatus.PRIVATE, VodStatus.DELETED);
    }

    // [상세 필터] 예약중/취소됨 등 상태 직접 필터링
    private BooleanExpression statusDetailFilter(String status) {
        if (status == null || "ALL".equalsIgnoreCase(status)) return null;
        try {
            return broadcast.status.eq(BroadcastStatus.valueOf(status));
        } catch (Exception e) {
            return null; // 유효하지 않은 상태값 무시
        }
    }

    private BooleanExpression tabCondition(String tab) {
        if (tab == null || "ALL".equalsIgnoreCase(tab)) return null;
        if ("LIVE".equalsIgnoreCase(tab)) return broadcast.status.in(BroadcastStatus.ON_AIR, BroadcastStatus.READY);
        if ("RESERVED".equalsIgnoreCase(tab)) return broadcast.status.in(BroadcastStatus.RESERVED, BroadcastStatus.CANCELED);
        if ("VOD".equalsIgnoreCase(tab)) return broadcast.status.in(BroadcastStatus.VOD, BroadcastStatus.ENDED, BroadcastStatus.STOPPED);
        return null;
    }

    // [정렬 로직]
    private OrderSpecifier<?> getOrderSpecifier(BroadcastSearch condition) {
        String sort = condition.getSortType();
        String tab = condition.getTab();

        // 1. 관리자 신고순
        if ("REPORT".equalsIgnoreCase(sort)) return new OrderSpecifier<>(Order.DESC, sanction.count());
        // 2. 판매자 매출순
        if ("SALES".equalsIgnoreCase(sort)) return new OrderSpecifier<>(Order.DESC, broadcastResult.totalSales);

        // 3. 인기순 (VOD: DB 조회수, LIVE: 시작시간 -> Redis는 메모리 정렬)
        if ("POPULAR".equalsIgnoreCase(sort) || "VIEWER".equalsIgnoreCase(sort)) {
            if ("VOD".equalsIgnoreCase(tab)) return new OrderSpecifier<>(Order.DESC, broadcastResult.totalViews);
            return new OrderSpecifier<>(Order.DESC, broadcast.startedAt);
        }

        // 4. 좋아요순
        if ("LIKE_DESC".equalsIgnoreCase(sort)) return new OrderSpecifier<>(Order.DESC, broadcastResult.totalLikes);
        if ("LIKE_ASC".equalsIgnoreCase(sort)) return new OrderSpecifier<>(Order.ASC, broadcastResult.totalLikes);

        // 5. 시작 임박순
        if ("RESERVED".equalsIgnoreCase(tab) || "START_ASC".equalsIgnoreCase(sort)) {
            return new OrderSpecifier<>(Order.ASC, broadcast.scheduledAt);
        }

        // 기본값: 최신순 (예약일 기준)
        return new OrderSpecifier<>(Order.DESC, broadcast.scheduledAt);
    }
}
