package com.example.LiveHost.others.entity;

import com.example.LiveHost.others.enums.TagCode;
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
@Table(name = "tag_category")
public class TagCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_category_id")
    private Long tagCategoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_code", nullable = false)
    private TagCode tagCode;

    @Column(name = "tag_category_name", nullable = false, length = 30)
    private String tagCategoryName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}