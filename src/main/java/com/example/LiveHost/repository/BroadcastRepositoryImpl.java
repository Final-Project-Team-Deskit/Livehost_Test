package com.example.LiveHost.repository;

import com.example.LiveHost.common.enums.BroadcastStatus;
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

import java.util.List;

import static com.example.LiveHost.entity.QBroadcast.broadcast;
import static com.example.LiveHost.entity.QBroadcastResult.broadcastResult;

@RequiredArgsConstructor
public class BroadcastRepositoryImpl implements BroadcastRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<BroadcastListResponse> searchBroadcasts(Long sellerId, BroadcastSearch condition, Pageable pageable) {
        List<BroadcastListResponse> content = queryFactory
                .select(Projections.constructor(BroadcastListResponse.class,
                        broadcast.broadcastId,
                        broadcast.broadcastTitle,
                        broadcast.broadcastThumbUrl,
                        broadcast.status,
                        broadcast.scheduledAt,
                        // [추가] 시작/종료 시간
                        broadcast.startedAt,
                        broadcast.endedAt,
                        // [추가] 결과 테이블의 총 조회수 (없으면 0 처리)
                        broadcastResult.totalViews.coalesce(0),
                        // [추가] 현재 시청자 (목록 조회 시점엔 0으로 두고 필요 시 Redis 값 채움)
                        com.querydsl.core.types.dsl.Expressions.constant(0)))
                .from(broadcast)
                // [핵심] 방송 결과 테이블 Left Join (예약 방송은 결과가 없으므로 Left Join 필수)
                .leftJoin(broadcastResult).on(broadcast.broadcastId.eq(broadcastResult.broadcastId))
                .where(
                        broadcast.sellerId.eq(sellerId),
                        tabCondition(condition.getTab()),        // [핵심] 탭별 상태 그룹 적용
                        keywordContains(condition.getKeyword()),
                        broadcast.status.ne(BroadcastStatus.DELETED)
                )
                .orderBy(getOrderSpecifier(condition)) // 정렬 조건 적용
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

    // 전체(ALL) 탭 대시보드용 미리보기 조회
    @Override
    public List<BroadcastListResponse> findRecentByStatusGroup(Long sellerId, String tabGroup, int limit) {
        return queryFactory
                .select(Projections.constructor(BroadcastListResponse.class,
                        broadcast.broadcastId,
                        broadcast.broadcastTitle,
                        broadcast.broadcastThumbUrl,
                        broadcast.status,
                        broadcast.scheduledAt,
                        broadcast.startedAt,  // [추가]
                        broadcast.endedAt,    // [추가]
                        broadcastResult.totalViews.coalesce(0), // [추가]
                        com.querydsl.core.types.dsl.Expressions.constant(0)
                ))
                .from(broadcast)
                .leftJoin(broadcastResult).on(broadcast.broadcastId.eq(broadcastResult.broadcastId)) // [추가] Join
                .where(
                        broadcast.sellerId.eq(sellerId),
                        tabCondition(tabGroup), // 여기도 동일한 그룹핑 로직 사용
                        broadcast.status.ne(BroadcastStatus.DELETED)
                )
                .orderBy(broadcast.scheduledAt.desc())
                .limit(limit)
                .fetch();
    }

    // [확인 완료] 말씀하신 그룹핑 조건이 여기에 반영되어 있습니다.
    private BooleanExpression tabCondition(String tab) {
        if ("LIVE".equalsIgnoreCase(tab)) {
            // LIVE 탭: ON_AIR(방송중), READY(대기), ENDED(종료-송출중단)
            return broadcast.status.in(BroadcastStatus.ON_AIR, BroadcastStatus.READY, BroadcastStatus.ENDED);
        } else if ("RESERVED".equalsIgnoreCase(tab)) {
            // RESERVED 탭: RESERVED(예약), CANCELED(취소)
            return broadcast.status.in(BroadcastStatus.RESERVED, BroadcastStatus.CANCELED);
        } else if ("VOD".equalsIgnoreCase(tab)) {
            // VOD 탭: VOD(다시보기), STOPPED(강제중지)
            return broadcast.status.in(BroadcastStatus.VOD, BroadcastStatus.STOPPED);
        }
        return broadcast.status.ne(BroadcastStatus.DELETED);
    }

    private BooleanExpression keywordContains(String keyword) {
        return keyword != null ? broadcast.broadcastTitle.contains(keyword) : null;
    }

    // 정렬 조건 (기본: 최신순)
    private OrderSpecifier<?> getOrderSpecifier(BroadcastSearch condition) {
        if ("VIEWER".equalsIgnoreCase(condition.getSortType())) {
            // TODO: 시청자순 정렬 로직 (DB 컬럼 or Redis 연동 필요)
        }
        return new OrderSpecifier<>(Order.DESC, broadcast.scheduledAt);
    }
}
