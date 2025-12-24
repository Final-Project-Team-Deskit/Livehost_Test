package com.example.LiveHost.service;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.exception.BusinessException;
import com.example.LiveHost.common.exception.ErrorCode;
import com.example.LiveHost.dto.BroadcastCreateRequest;
import com.example.LiveHost.dto.BroadcastProductRequest;
import com.example.LiveHost.dto.BroadcastUpdateRequest;
import com.example.LiveHost.dto.QcardRequest;
import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.entity.BroadcastProduct;
import com.example.LiveHost.entity.Qcard;
import com.example.LiveHost.repository.BroadcastProductRepository;
import com.example.LiveHost.repository.BroadcastRepository;
import com.example.LiveHost.repository.QcardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastService {

    private final BroadcastRepository broadcastRepository;
    private final BroadcastProductRepository broadcastProductRepository;
    private final QcardRepository qcardRepository;

    // 방송 생성 (POST)
      // 기능: 방송 정보 저장 + 상품 매핑 + 큐카드 저장
      // 정책: 예약된 방송은 판매자당 최대 7개까지만 가능
    @Transactional
    public Long createBroadcast(Long sellerId, BroadcastCreateRequest request) {

        // 1. [검증] 예약된 방송 7개 제한 체크
        long reservedCount = broadcastRepository.countBySellerIdAndBroadcastStatus(sellerId, BroadcastStatus.RESERVED);
        if (reservedCount >= 7) {
            throw new BusinessException(ErrorCode.RESERVATION_LIMIT_EXCEEDED);
        }

        // 2. Broadcast Entity 생성 및 저장
        // (주의: DTO에 toEntity 메서드가 없다면 Builder로 직접 생성)
        Broadcast broadcast = Broadcast.builder()
                .sellerId(sellerId)
                .categoryId(request.getCategoryId())
                .broadcastTitle(request.getTitle())
                .broadcastNotice(request.getNotice())
                .scheduledAt(request.getScheduledAt())
                .broadcastThumbUrl(request.getThumbnailUrl())
                .broadcastWaitUrl(request.getWaitScreenUrl())
                .broadcastLayout(request.getBroadcastLayout())
                .broadcastStatus(BroadcastStatus.RESERVED) // 초기 상태는 예약
                .build();

        Broadcast savedBroadcast = broadcastRepository.save(broadcast);

        // 3. 방송-상품 매핑 저장
        saveBroadcastProducts(savedBroadcast, request.getProducts());

        // 4. 큐카드 저장 (옵션)
        saveQcards(savedBroadcast, request.getQcards());

        log.info("방송 생성 완료: id={}, title={}", savedBroadcast.getBroadcastId(), savedBroadcast.getBroadcastTitle());
        return savedBroadcast.getBroadcastId();
    }


    // 방송 정보 수정 (PUT)
    @Transactional
    public Long updateBroadcast(Long sellerId, Long broadcastId, BroadcastUpdateRequest request) {
        // 1. 방송 조회
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        // 2. 권한 검증 (Aspect가 해주지만, Service에서도 이중 체크 권장)
        if (!broadcast.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 3. 상태 검증 (예약 상태일 때만 수정 가능)
        if (broadcast.getBroadcastStatus() != BroadcastStatus.RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE); // "이미 시작된 방송은 수정 불가" 등의 메시지 필요
        }

        // 4. 방송 기본 정보 수정
        broadcast.updateBroadcastInfo(
                request.getCategoryId(),
                request.getTitle(),
                request.getNotice(),
                request.getScheduledAt(),
                request.getThumbnailUrl(),
                request.getWaitScreenUrl(),
                request.getBroadcastLayout()
        );

        // 5. 상품/큐카드 수정 (전체 삭제 후 재등록 전략)
        updateBroadcastProducts(broadcast, request.getProducts());
        updateQcards(broadcast, request.getQcards());

        return broadcast.getBroadcastId();

        // 트랜잭션이 닫힐 때, JPA가 값이 달라진 것을 감지하고, 자동으로 UPDATE 쿼리를 생성해서 DB에 날림
    }

    // [방송 예약 취소 (DELETE)
    @Transactional
    public void cancelBroadcast(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        // 권한 체크
        if (!broadcast.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 상태 체크 (예약 상태일 때만 삭제 가능)
        if (broadcast.getBroadcastStatus() != BroadcastStatus.RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 상태를 DELETED로 변경
        broadcast.delete();

        log.info("방송 삭제(취소) 처리 완료: id={}, status={}", broadcastId, broadcast.getBroadcastStatus());

        // 트랜잭션이 닫힐 때, JPA가 값이 달라진 것을 감지하고, 자동으로 UPDATE 쿼리를 생성해서 DB에 날림
    }


    // --- Private Helper Methods (코드 가독성을 위해 분리) ---
    private void saveBroadcastProducts(Broadcast broadcast, List<BroadcastProductRequest> products) {
        if (products == null || products.isEmpty()) return;

        // 한 번에 저장하는 것이 성능상 좋지만, 로직이 단순하므로 반복문 save 사용 (JPA Batch Insert는 별도 설정 필요)
        for (BroadcastProductRequest dto : products) {
            BroadcastProduct product = BroadcastProduct.builder()
                    .broadcast(broadcast) // 연관관계 설정
                    .productId(dto.getProductId())
                    .bpPrice(dto.getLivePrice())        // 방송 특가
                    .bpQuantity(dto.getSaleQuantity())  // 방송 재고
                    .isPinned(false) // 초기엔 핀 없음
                    .build();

            broadcastProductRepository.save(product);
        }
    }

    private void saveQcards(Broadcast broadcast, List<QcardRequest> qcards) {
        if (qcards == null || qcards.isEmpty()) return;

        int order = 1; // 1번부터 순서 부여
        for (QcardRequest dto : qcards) {
            Qcard qcard = Qcard.builder()
                    .broadcast(broadcast)
                    .sortOrder(order++)       // 1, 2, 3...
                    .qcardQuestion(dto.getQuestion())
                    .build();

            qcardRepository.save(qcard);
        }
    }

    private void updateBroadcastProducts(Broadcast broadcast, List<BroadcastProductRequest> products) {
        // 1. 기존 상품 싹 지우기
        broadcastProductRepository.deleteByBroadcast(broadcast);

        // 2. 새 상품 등록 (create 때와 동일 로직)
        if (products != null && !products.isEmpty()) {
            for (BroadcastProductRequest dto : products) {
                BroadcastProduct product = BroadcastProduct.builder()
                        .broadcast(broadcast)
                        .productId(dto.getProductId())
                        .bpPrice(dto.getLivePrice())
                        .bpQuantity(dto.getSaleQuantity())
                        .isPinned(false)
                        .build();
                broadcastProductRepository.save(product);
            }
        }
    }

    private void updateQcards(Broadcast broadcast, List<QcardRequest> qcards) {
        qcardRepository.deleteByBroadcast(broadcast);

        if (qcards != null && !qcards.isEmpty()) {
            int order = 1;
            for (QcardRequest dto : qcards) {
                Qcard qcard = Qcard.builder()
                        .broadcast(broadcast)
                        .sortOrder(order++)
                        .qcardQuestion(dto.getQuestion())
                        .build();
                qcardRepository.save(qcard);
            }
        }
    }
}
