package com.example.LiveHost.others.entity;

import com.example.LiveHost.others.enums.Mbti;
import com.example.LiveHost.others.enums.MemberStatus;
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
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "login_id", nullable = false, length = 100)
    private String loginId;

    // [수정] SQL 컬럼명: profile
    @Column(name = "profile", length = 500)
    private String profile;

    @Column(nullable = false, length = 15)
    private String phone;

    // SQL: TINYINT -> Java: boolean
    @Column(name = "is_agreed", nullable = false, columnDefinition = "TINYINT")
    private boolean isAgreed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    // [수정] SQL ENUM 정의에 맞춘 Enum 사용
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private Mbti mbti = Mbti.NONE;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "ROLE_MEMBER";

    // [수정] SQL ENUM('사무/기획형'...) 한글 처리를 위해 String 사용 권장
    @Column(name = "job_category", columnDefinition = "ENUM('사무/기획형', '창의/디자인형', '교육/연구형', '의료/전문서비스형', '자유/유연형', 'NONE')")
    @Builder.Default
    private String jobCategory = "NONE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
