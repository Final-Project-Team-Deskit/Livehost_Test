package com.example.LiveHost.repository;

import com.example.LiveHost.dto.StatisticsResponse;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import static com.example.LiveHost.entity.QBroadcast.broadcast;
import static com.example.LiveHost.entity.QBroadcastResult.broadcastResult;

@RequiredArgsConstructor
public class BroadcastResultRepositoryImpl implements BroadcastResultRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // 1. 기간별 매출 차트 조회
    @Override
    public List<StatisticsResponse.ChartData> getSalesChart(Long sellerId, String periodType) {
        StringTemplate dateExpr = getDateExpression(periodType);

        return queryFactory
                .select(Projections.constructor(StatisticsResponse.ChartData.class,
                        dateExpr,                       // X축
                        broadcastResult.totalSales.sum() // Y축
                ))
                .from(broadcastResult)
                .join(broadcastResult.broadcast, broadcast)
                .where(
                        sellerIdEq(sellerId),
                        broadcast.endedAt.isNotNull()
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch();
    }

    // 2. 기간별 ARPU(객단가) 차트 조회
    @Override
    public List<StatisticsResponse.ChartData> getArpuChart(Long sellerId, String periodType) {
        StringTemplate dateExpr = getDateExpression(periodType);

        // ARPU = SUM(Sales) / SUM(Views)
        NumberExpression<BigDecimal> arpu = broadcastResult.totalSales.sum()
                .divide(broadcastResult.totalViews.sum().nullif(0));

        return queryFactory
                .select(Projections.constructor(StatisticsResponse.ChartData.class,
                        dateExpr,
                        arpu
                ))
                .from(broadcastResult)
                .join(broadcastResult.broadcast, broadcast)
                .where(
                        sellerIdEq(sellerId),
                        broadcast.endedAt.isNotNull()
                )
                .groupBy(dateExpr)
                .orderBy(dateExpr.asc())
                .fetch();
    }

    // 3. 방송 랭킹 조회 (메서드명 getRanking으로 통일)
    @Override
    public List<StatisticsResponse.BroadcastRank> getRanking(Long sellerId, String sortField, boolean isDesc, int limit) {
        return queryFactory
                .select(Projections.constructor(StatisticsResponse.BroadcastRank.class,
                        broadcast.broadcastId,
                        broadcast.broadcastTitle,
                        broadcastResult.totalSales,
                        broadcastResult.totalViews
                ))
                .from(broadcastResult)
                .join(broadcastResult.broadcast, broadcast)
                .where(
                        sellerIdEq(sellerId),
                        broadcast.endedAt.isNotNull()
                )
                .orderBy(getOrderSpecifier(sortField, isDesc))
                .limit(limit)
                .fetch();
    }

    // --- Helper Methods ---

    private BooleanExpression sellerIdEq(Long sellerId) {
        return sellerId != null ? broadcast.seller.sellerId.eq(sellerId) : null;
    }

    private StringTemplate getDateExpression(String periodType) {
        String format = "DAILY".equalsIgnoreCase(periodType) ? "%Y-%m-%d" :
                "MONTHLY".equalsIgnoreCase(periodType) ? "%Y-%m" : "%Y";
        return Expressions.stringTemplate("DATE_FORMAT({0}, {1})", broadcast.endedAt, format);
    }

    private OrderSpecifier<?> getOrderSpecifier(String sortField, boolean isDesc) {
        if ("SALES".equalsIgnoreCase(sortField)) {
            return isDesc ? broadcastResult.totalSales.desc() : broadcastResult.totalSales.asc();
        } else {
            return isDesc ? broadcastResult.totalViews.desc() : broadcastResult.totalViews.asc();
        }
    }
}