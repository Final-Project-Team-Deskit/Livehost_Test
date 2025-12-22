package com.example.LiveHost.common.utils;

import com.example.LiveHost.common.exception.BusinessException;
import com.example.LiveHost.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    // 현재 로그인한 유저의 ID(PK)를 반환
    public static Long getCurrentUserId() {
        // 1. 금고(SecurityContext)에서 인증 정보 꺼내기
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. 인증 정보가 없거나, 익명 사용자(로그인 안 함)라면 에러
        if (authentication == null || authentication.getName() == null ||
                authentication.getName().equals("anonymousUser")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_MEMBER);
        }

        // 3. JWT 필터에서 저장해둔 ID 꺼내기 (보통 Name에 ID를 문자열로 저장함)
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}
