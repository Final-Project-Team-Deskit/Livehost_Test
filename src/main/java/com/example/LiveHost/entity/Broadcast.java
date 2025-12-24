package com.example.LiveHost.entity;

import com.example.LiveHost.common.enums.BroadcastLayout;
import com.example.LiveHost.common.enums.BroadcastStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "Broadcast")
public class Broadcast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "broadcast_id")
    private Long broadcastId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId; // 판매자 ID

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "broadcast_title", length = 30, nullable = false)
    private String broadcastTitle;

    @Column(name = "broadcast_notice", length = 100)
    private String broadcastNotice;

    @Enumerated(EnumType.STRING)
    @Column(name = "broadcast_status", nullable = false)
    private BroadcastStatus broadcastStatus;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "broadcast_thumb_url", nullable = false)
    private String broadcastThumbUrl;

    @Column(name = "broadcast_wait_url")
    private String broadcastWaitUrl;

    // [중요] 테이블의 stream_key를 자바에서는 sessionId로 사용 (OpenVidu)
    @Column(name = "stream_key", length = 100)
    private String streamKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "broadcast_layout", nullable = false)
    private BroadcastLayout broadcastLayout;

    @Column(name = "broadcast_stopped_reason", length = 50)
    private String broadcastStoppedReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 양방향 매핑 (상품, 큐카드)
    // @OneToMany : 일(방송)대다(상품, 큐카드) 관계
    // mappedBy = "Broadcast" : Product의 broadcast 필드가 FK를 관리
    // cascade = CascadeType.ALL : 방송 지울 떼 그에 해당하는 방송 상품들도 같이 지움
    @OneToMany(mappedBy = "broadcast", cascade = CascadeType.PERSIST)
    private List<BroadcastProduct> products = new ArrayList<>();

    @OneToMany(mappedBy = "broadcast", cascade = CascadeType.PERSIST)
    private List<Qcard> qcards = new ArrayList<>();

    public void deleteBroadcast() {
        this.broadcastStatus = BroadcastStatus.DELETED;
    }

    /**
     * [비즈니스 로직] 방송 정보 수정
     * - 수정 가능한 필드만 파라미터로 받아서 변경
     * - null이 들어오지 않도록 DTO에서 검증했으므로 바로 대입
     */
    public void updateBroadcastInfo(Long categoryId, String title, String notice,
                                    LocalDateTime scheduledAt, String thumbUrl,
                                    String waitUrl, BroadcastLayout layout) {
        this.categoryId = categoryId;
        this.broadcastTitle = title;
        this.broadcastNotice = notice;
        this.scheduledAt = scheduledAt;
        this.broadcastThumbUrl = thumbUrl;
        this.broadcastWaitUrl = waitUrl;
        this.broadcastLayout = layout;
    }

    public void delete() {
        this.broadcastStatus = BroadcastStatus.DELETED;
    }
}
