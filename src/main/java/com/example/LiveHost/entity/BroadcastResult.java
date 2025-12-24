package com.example.LiveHost.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "broadcast_result")
public class BroadcastResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "broadcast_id")
    private Long broadcastId;

    @Column(name = "total_views", nullable = false)
    private int totalViews;

    @Column(name = "max_views", nullable = false)
    private int maxViews;

    @Column(name = "max_views_at")
    private LocalDateTime pickViewsAt;

    @Column(name = "total_likes", nullable = false)
    private int totalLikes;

    @Column(name = "total_chats", nullable = false)
    private int totalChats;

    @Column(name = "total_sales", nullable = false, precision = 30, scale = 0)
    private BigDecimal totalSales; // 금액은 BigDecimal 사용 권장

    @Column(name = "avg_watch_time", nullable = false)
    private int avgWatchTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
