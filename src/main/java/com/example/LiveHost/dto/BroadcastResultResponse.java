package com.example.LiveHost.dto;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.enums.VodStatus;
import com.example.LiveHost.entity.BroadcastResult;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BroadcastResultResponse {
    private Long broadcastId;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private long durationMinutes;
    private BroadcastStatus status;
    private String stoppedReason;

    private int totalViews;
    private int totalLikes;
    private BigDecimal totalSales;
    private int totalChats;
    private int maxViewers;
    private LocalDateTime maxViewerTime;
    private long avgWatchTime;
    private int reportCount;
    private int sanctionCount;

    private String vodUrl;
    private VodStatus vodStatus;
    private boolean isEncoding;

    private List<ProductSalesStat> productStats;

    @Getter @Builder
    public static class ProductSalesStat {
        private Long productId;
        private String productName;
        private String imageUrl;
        private int price;
        private int salesQuantity;
        private BigDecimal salesAmount;
    }
}
