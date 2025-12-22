package com.example.LiveHost.entity;

import com.example.LiveHost.common.enums.BroadcastProductStatus;
import com.example.LiveHost.common.utils.BooleanToYNConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "BroadcastProduct")
public class BroadcastProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bp_id")
    private Long bpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broadcast_id", nullable = false)
    private Broadcast broadcast;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "bp_price")
    private Integer bpPrice; // 라이브 특가

    @Column(name = "bp_quantity", nullable = false)
    private int bpQuantity;

    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "is_pinned", nullable = false, length = 1)
    private boolean isPinned; // DB엔 'Y'/'N', 자바엔 true/false

    @Enumerated(EnumType.STRING)
    @Column(name = "bp_status", nullable = false)
    private BroadcastProductStatus bpStatus;
}
