package com.example.LiveHost.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QcardRequest {
    @NotBlank(message = "질문 내용은 필수입니다.") // 테이블: qcard_question
    @Size(max = 50, message = "큐카드 질문은 최대 50자까지 입력 가능합니다.") // ★ 테이블 정의서 반영 (50자)
    private String question;

    // sort_order(정렬 순서)는 List의 인덱스로 Service에서 처리
}
