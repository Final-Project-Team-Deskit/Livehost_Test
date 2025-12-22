package com.example.LiveHost.entity;

import com.example.LiveHost.common.enums.ActorType;
import com.example.LiveHost.common.enums.SanctionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "Sanction")
public class Sanction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sanction_id")
    private Long sanctionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broadcast_id", nullable = false)
    private Broadcast broadcast;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    private ActorType actorType;

    @Column(name = "customer_id")
    private Long sellerId; // 판매자 ID

    @Column(name = "admin_id")
    private Long adminId; // 관리자 ID

    @Enumerated(EnumType.STRING)
    @Column(name = "sanction_status", nullable = false)
    private SanctionType sanctionStatus;

    @Column(name = "sanction_reason", length = 50)
    private String sanctionReason;

    @Column(name = "started_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime startedAt;
}
