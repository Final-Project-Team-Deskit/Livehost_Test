package com.example.LiveHost.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode { // 에러 코드 모음
    // 1. 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),

    // 2. 인증/권한 에러
    UNAUTHORIZED_MEMBER(HttpStatus.UNAUTHORIZED, "A001", "로그인이 필요합니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    // [JWT 토큰 관련 에러] - 담당자가 던질 수 있음
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A005", "리프레시 토큰이 없습니다."),

    // 3. 방송(Broadcast) 에러
    BROADCAST_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "방송을 찾을 수 없습니다."),
    RESERVATION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "B002", "예약은 최대 7개까지만 가능합니다."),

    // 4. 상품(Product) 에러
    PRODUCT_SOLD_OUT(HttpStatus.BAD_REQUEST, "P001", "품절된 상품은 핀 설정이 불가능합니다."),

    // 5. OpenVidu 에러
    OPENVIDU_SESSION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "O001", "세션 생성 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
