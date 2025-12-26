package com.example.LiveHost.others.entity;

import com.example.LiveHost.others.enums.ProductStatus;
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

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "short_desc", nullable = false, length = 250)
    private String shortDesc;

    // SQL: LONGTEXT -> Java: @Lob
    @Lob
    @Column(name = "detail_html", nullable = false, columnDefinition = "LONGTEXT")
    private String detailHtml;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "cost_price", nullable = false)
    @Builder.Default
    private Integer costPrice = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "stock_qty", nullable = false)
    @Builder.Default
    private Integer stockQty = 0;

    @Column(name = "safety_stock", nullable = false)
    @Builder.Default
    private Integer safetyStock = 5;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 양방향 매핑 (이미지)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    // 편의 메서드: 썸네일 URL 반환
    public String getProductThumbUrl() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(img -> img.getSlotIndex() == 0)
                .findFirst()
                .map(ProductImage::getProductImageUrl)
                .orElse(images.get(0).getProductImageUrl());
    }

    public Long getSellerId() {
        return this.seller != null ? this.seller.getSellerId() : null;
    }
}