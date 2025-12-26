package com.example.LiveHost.service;

import com.example.LiveHost.common.enums.BroadcastProductStatus;
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
import com.example.LiveHost.others.entity.Product;
import com.example.LiveHost.others.repository.ProductRepository;
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
    private final ProductRepository productRepository;

    // 방송 생성 (POST)
      // 기능: 방송 정보 저장 + 상품 매핑 + 큐카드 저장
      // 정책: 예약된 방송은 판매자당 최대 7개까지만 가능
    @Transactional
    public Long createBroadcast(Long sellerId, BroadcastCreateRequest request) {

        // 1. [검증] 예약된 방송 7개 제한 체크
        long reservedCount = broadcastRepository.countBySellerIdAndStatus(sellerId, BroadcastStatus.RESERVED);
        if (reservedCount >= 7) {
            throw new BusinessException(ErrorCode.RESERVATION_LIMIT_EXCEEDED);
        }

        // 2. Broadcast Entity 생성 및 저장
        Broadcast broadcast = request.toEntity(sellerId);
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
        if (broadcast.getStatus() != BroadcastStatus.RESERVED) {
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
        updateBroadcastProducts(sellerId, broadcast, request.getProducts());
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
        if (broadcast.getStatus() != BroadcastStatus.RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 상태를 DELETED로 변경
        broadcast.delete();

        log.info("방송 삭제(취소) 처리 완료: id={}, status={}", broadcastId, broadcast.getStatus());

        // 트랜잭션이 닫힐 때, JPA가 값이 달라진 것을 감지하고, 자동으로 UPDATE 쿼리를 생성해서 DB에 날림
    }


    // --- Private Helper Methods (코드 가독성을 위해 분리) ---
    private void saveBroadcastProducts(Broadcast broadcast, List<BroadcastProductRequest> products) {
        if (products == null || products.isEmpty()) return;

        int displayOrder = 1;
        for (BroadcastProductRequest dto : products) {
            BroadcastProduct product = BroadcastProduct.builder()
                    .broadcast(broadcast)
                    .productId(dto.getProductId())
                    .bpPrice(dto.getLivePrice())       // DTO: livePrice -> Entity: bpPrice
                    .bpQuantity(dto.getSaleQuantity()) // DTO: saleQuantity -> Entity: bpQuantity
                    .displayOrder(displayOrder++)      // 1부터 순차 증가
                    .isPinned(false)                   // 초기 고정 상태 false
                    .status(BroadcastProductStatus.SELLING)
                    .build();

            broadcastProductRepository.save(product);
        }
    }

    private void saveQcards(Broadcast broadcast, List<QcardRequest> qcards) {
        if (qcards == null || qcards.isEmpty()) return;

        int sortOrder = 1;
        for (QcardRequest dto : qcards) {
            Qcard qcard = Qcard.builder()
                    .broadcast(broadcast)
                    .qcardQuestion(dto.getQuestion())
                    .sortOrder(sortOrder++) // 1부터 순차 증가
                    .build();

            qcardRepository.save(qcard);
        }
    }

    private void updateBroadcastProducts(Long sellerId, Broadcast broadcast, List<BroadcastProductRequest> products) {
        // 1. 기존 상품 싹 지우기
        broadcastProductRepository.deleteByBroadcast(broadcast);

        // 2. 새 상품 등록 (create 때와 동일 로직)
        if (products != null && !products.isEmpty()) {
            for (BroadcastProductRequest dto : products) {
                // [검증 1] 상품 존재 여부
                Product product = productRepository.findById(dto.getProductId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

                // [검증 2] 내 상품인지 확인 (SellerId 비교)
                // Product 엔티티에 getSellerId() 편의 메서드가 없으면 product.getSeller().getSellerId() 사용
                if (!product.getSeller().getSellerId().equals(sellerId)) {
                    throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS); // 본인 상품만 등록 가능
                }

                BroadcastProduct bp = BroadcastProduct.builder()
                        .broadcast(broadcast)
                        .productId(dto.getProductId())
                        .bpPrice(dto.getLivePrice())
                        .bpQuantity(dto.getSaleQuantity())
                        .isPinned(false)
                        .status(BroadcastProductStatus.SELLING)
                        .build();
                broadcastProductRepository.save(bp);
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
