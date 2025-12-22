package com.example.LiveHost.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j // Simple Logging Facade for Java : 다양한 로깅 프레임 워크에 대한 추상화 역할을 하는 라이브러리
@RestControllerAdvice // 컨트롤러에서 발생하는 예외를 가로챔
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler { // 전역 핸들러

    // 1. 비즈니스 로직 에러 (우리가 throw한 것)
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResult<?>> handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResult.error(e.getErrorCode()));
    }

    // 2. @Valid 유효성 검사 실패 (입력값 이상)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResult<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Validation Error: {}", e.getMessage());
        return ResponseEntity
                .status(400)
                .body(ApiResult.error(ErrorCode.INVALID_INPUT_VALUE));
    }

    // 3. 그 외 알 수 없는 에러 (최후의 보루)
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResult<?>> handleException(Exception e) {
        log.error("Unknown Exception: ", e);
        return ResponseEntity
                .status(500)
                .body(ApiResult.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}