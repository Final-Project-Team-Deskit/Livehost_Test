package com.example.LiveHost.others.entity;

import com.example.LiveHost.others.enums.SellerStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "seller")
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seller_id")
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SellerStatus status = SellerStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "login_id", nullable = false, length = 100)
    private String loginId;

    @Column(nullable = false, length = 15)
    private String phone;

    // [수정] SQL 컬럼명: profile
    @Column(name = "profile", length = 500)
    private String profile;

    @Column(nullable = false, length = 20)
    private String role;
}