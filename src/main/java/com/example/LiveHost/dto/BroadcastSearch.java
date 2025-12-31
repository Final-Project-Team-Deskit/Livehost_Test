package com.example.LiveHost.dto;

import com.example.LiveHost.common.enums.BroadcastStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class BroadcastSearch {
    // --- 기본 필터 ---
    private String tab; // ALL, RESERVED, LIVE, VOD
    private String keyword;

    // --- 상세 필터 ---
    private String sortType; // LATEST, POPULAR(인기), SALES(매출), REPORT(신고), START_ASC(시작임박)
    private Long categoryId; // 카테고리
    private Boolean isPublic; // 공개/비공개 필터 (VOD용)
    private String statusFilter; // 세부 상태 필터 (예: CANCELED만 보기 등)

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate; // 조회 시작 날짜

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;   // 조회 종료 날짜

    // --- [무한 스크롤용] ---
    // Controller에서 Pageable 객체로 받을 수도 있지만, DTO로 받는 경우를 위해 유지
    private int page = 0;
    private int size = 10;
}
