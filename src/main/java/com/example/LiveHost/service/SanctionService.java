package com.example.LiveHost.service;

import com.example.LiveHost.common.enums.SanctionType;
import com.example.LiveHost.common.exception.BusinessException;
import com.example.LiveHost.common.exception.ErrorCode;
import com.example.LiveHost.dto.SanctionResponse;
import com.example.LiveHost.dto.request.SanctionRequest;
import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.entity.Sanction;
import com.example.LiveHost.others.entity.Member;
import com.example.LiveHost.others.repository.MemberRepository;
import com.example.LiveHost.repository.BroadcastRepository;
import com.example.LiveHost.repository.SanctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SanctionService {

    private final SanctionRepository sanctionRepository;
    private final BroadcastRepository broadcastRepository;
    private final MemberRepository memberRepository;
    private final RedisService redisService;
    private final SseService sseService;
    private final OpenViduService openViduService;

    @Transactional
    public void sanctionUser(Long sellerId, Long broadcastId, SanctionRequest request) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Sanction sanction = Sanction.builder()
                .broadcast(broadcast)
                .member(member)
                .actorType(request.getActorType())
                .sellerId(sellerId)
                .status(request.getStatus()) // MUTE or OUT
                .sanctionReason(request.getReason())
                .build();

        sanctionRepository.save(sanction);

        redisService.increment(redisService.getSanctionKey(broadcastId));

        // -----------------------------------------------------------
        // [핵심] 강퇴(OUT) 처리 로직
        // -----------------------------------------------------------
        if (request.getStatus() == SanctionType.OUT && request.getConnectionId() != null) {
            // OpenVidu 세션에서 강제로 연결을 끊어버림 -> 시청자는 방송 화면에서 튕겨나감
            openViduService.forceDisconnect(broadcastId, request.getConnectionId());
        }

        // -----------------------------------------------------------
        // [수정] 알림 전송 로직 강화
        // -----------------------------------------------------------

        // 1. 제재 당사자에게 알림 ("당신은 강퇴되었습니다" -> 프론트에서 튕겨내기)
        sseService.notifyTargetUser(
                broadcastId,
                request.getMemberId(),
                "SANCTION_ALERT", // 이벤트명
                Map.of(
                        "type", request.getStatus(), // MUTE or OUT
                        "reason", request.getReason()
                )
        );

        // 2. (선택) 전체 시청자에게 알림 ("OOO님이 제재되었습니다" -> 채팅 가리기용)
        // 채팅창 관리를 위해 필요하다면 유지, 아니면 제거 가능
        sseService.notifyBroadcastUpdate(
                broadcastId,
                "SANCTION_UPDATED",
                Map.of("targetMemberId", request.getMemberId())
        );
    }

    @Transactional(readOnly = true)
    public List<SanctionResponse> getSanctions(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return sanctionRepository.findByBroadcastOrderByCreatedAtDesc(broadcast).stream()
                .map(sanction -> SanctionResponse.fromEntity(sanction, sanction.getMember().getName()))
                .collect(Collectors.toList());
    }
}
