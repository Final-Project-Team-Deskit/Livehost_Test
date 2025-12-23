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
    private String tab; // RESERVED, LIVE, VOD
    private String keyword;

    // --- 상세 필터 ---
    private Boolean includeCanceled;
    private String sortType; // LATEST, VIEWER, REPORT
    private String vodVisibility;
    private Long categoryId;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // --- [무한 스크롤용] ---
    // 0부터 시작. 스크롤이 바닥에 닿을 때마다 +1 해서 요청
    private int page = 0;
    private int size = 10; // 한 번에 가져올 개수
}
