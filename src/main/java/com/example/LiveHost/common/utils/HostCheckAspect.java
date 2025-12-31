package com.example.LiveHost.common.utils;

import com.example.LiveHost.common.exception.BusinessException;
import com.example.LiveHost.common.exception.ErrorCode;
import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.repository.BroadcastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;


import java.util.Arrays;

@Slf4j
@Aspect  // AOP 등록
@Component // 빈 등록
@RequiredArgsConstructor
public class HostCheckAspect {
    private final BroadcastRepository broadcastRepository;

    // @HostCheck가 붙은 메소드가 실행되기 직전에 이 로직을 실행
    @Before("@annotation(com.example.LiveHost.common.utils.HostCheck)")
    public void checkHostAuthority(JoinPoint joinPoint) { // JoinPoint : AOP가 가로챈 순간의 메소드 실행 정보가 담긴 객체
        // 1. 요청 파라미터에서 broadcastId(방송 ID) 찾기
        Long broadcastId = findBroadcastId(joinPoint.getArgs()); // JoinPoint 파라미터 확인
        // joinPoint.getSignature() : 실행하려던 메소드 이름 확인
        // joinPoint.getTarget() : 어떤 객체의 메소드인지 확인


        // 2. 현재 요청을 보낸 유저 ID 가져오기
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 3. 방송 조회 (없으면 404 에러)
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        // 4. 방송의 판매자ID와 현재 유저ID가 같은지 비교
        if (!broadcast.getSeller().getSellerId().equals(currentUserId)) {
            log.warn("권한 없는 접근 시도! 방송ID: {}, 시도한 유저ID: {}", broadcastId, currentUserId);
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS); // 주인 X (403 에러)
        }
    }

    // 파라미터 배열에서 Long 타입(방송 ID)을 찾는 메서드
    private Long findBroadcastId(Object[] args) {
        // 파라미터들 중 첫 번째로 발견되는 Long 타입을 broadcastId로 간주
        return Arrays.stream(args)
                .filter(arg -> arg instanceof Long)
                .map(arg -> (Long) arg)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
    }
}