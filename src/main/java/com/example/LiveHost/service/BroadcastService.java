package com.example.LiveHost.service;

import com.example.LiveHost.common.enums.BroadcastProductStatus;
import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.exception.BusinessException;
import com.example.LiveHost.common.exception.ErrorCode;
import com.example.LiveHost.dto.*;
import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.entity.BroadcastProduct;
import com.example.LiveHost.entity.Qcard;
import com.example.LiveHost.others.entity.Product;
import com.example.LiveHost.others.entity.TagCategory;
import com.example.LiveHost.others.repository.ProductRepository;
import com.example.LiveHost.others.repository.TagCategoryRepository;
import com.example.LiveHost.repository.BroadcastProductRepository;
import com.example.LiveHost.repository.BroadcastRepository;
import com.example.LiveHost.repository.QcardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastService {

    private final BroadcastRepository broadcastRepository;
    private final BroadcastProductRepository broadcastProductRepository;
    private final QcardRepository qcardRepository;
    private final TagCategoryRepository tagCategoryRepository;
    private final ProductRepository productRepository;
    private final RedisService redisService;

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
        saveBroadcastProducts(sellerId, savedBroadcast, request.getProducts());

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

    //  내 상품 목록 조회 (방송 등록 화면 팝업용)
    @Transactional(readOnly = true)
    public List<ProductSelectResponse> getSellerProducts(Long sellerId, String keyword) {
        List<Product> products = (keyword == null || keyword.isBlank())
                ? productRepository.findAllAvailableBySellerId(sellerId)
                : productRepository.searchAvailableBySellerId(sellerId, keyword);

        // Entity -> DTO 변환
        return products.stream()
                .map(p -> ProductSelectResponse.builder()
                        .productId(p.getProductId())
                        .productName(p.getProductName())
                        .price(p.getPrice())
                        .stockQty(p.getStockQty())
                        .imageUrl(p.getProductThumbUrl())
                        .build())
                .collect(Collectors.toList());
    }

    // 방송 상세 조회 (수정 화면)
    @Transactional(readOnly = true)
    public BroadcastResponse getBroadcastDetail(Long sellerId, Long broadcastId) {
        // 1. 방송 조회
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        // 2. 권한 검증 (내 방송인지)
        if (!broadcast.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 3. 카테고리 이름 조회
        String categoryName = "카테고리 없음";
        if (broadcast.getTagCategoryId() != null) {
            categoryName = tagCategoryRepository.findById(broadcast.getTagCategoryId())
                    .map(TagCategory::getTagCategoryName)
                    .orElse("삭제된 카테고리");
        }

        // 4. 상품 리스트 변환 (상태값 포함)
        List<BroadcastProductResponse> productList = getProductListResponse(broadcast);

        // 5. 큐카드 리스트 변환
        List<QcardResponse> qcardList = getQcardListResponse(broadcast);

        // 6. 응답 생성 (통계는 0으로 고정)
        return BroadcastResponse.fromEntity(
                broadcast,
                categoryName,
                0, // Total Views (수정 화면에선 불필요)
                0, // Total Likes (수정 화면에선 불필요)
                0,
                productList,
                qcardList
        );
    }

    /**
     * [시청자/대시보드용] 실시간 방송 상세 조회
     * - 특징: 방송 상태에 따라 Redis에서 실시간 데이터(조회수, 좋아요, 제재 건수)를 가져옴
     */
    @Transactional(readOnly = true)
    public BroadcastResponse getLiveBroadcastDetail(Long broadcastId) {

        // 1. 방송 조회
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        // 2. 카테고리 이름 조회
        String categoryName = "카테고리 없음";
        if (broadcast.getTagCategoryId() != null) {
            categoryName = tagCategoryRepository.findById(broadcast.getTagCategoryId())
                    .map(TagCategory::getTagCategoryName).orElse("삭제된 카테고리");
        }

        // 3. [핵심] 상태별 통계 데이터 조회 (Redis 연동)
        Integer totalViews = 0;
        Integer totalLikes = 0;
        Integer totalSanctions = 0; // 제재 건수 초기화

        if (broadcast.getStatus() == BroadcastStatus.ON_AIR) {
            // [방송 중] Redis에서 실시간 데이터 조회
            totalViews = redisService.getStatOrZero(redisService.getViewKey(broadcastId));
            totalLikes = redisService.getStatOrZero(redisService.getLikeKey(broadcastId));
            totalSanctions = redisService.getStatOrZero(redisService.getSanctionKey(broadcastId)); // 제재 건수
        }
        else if (broadcast.getStatus() == BroadcastStatus.ENDED) {
            // [방송 종료] DB 결과 테이블(BroadcastResult)에서 조회 (Entity 있다면)
            // BroadcastResult result = broadcastResultRepository.findById(broadcastId).orElse(null);
            // if (result != null) { ... 값 세팅 ... }
        }
        // RESERVED(예약) 상태는 모두 0으로 유지

        // 4. 상품 및 큐카드 리스트 변환 (Helper 메서드 활용 추천)
        List<BroadcastProductResponse> productList = getProductListResponse(broadcast);
        List<QcardResponse> qcardList = getQcardListResponse(broadcast);

        // 5. 응답 반환
        return BroadcastResponse.fromEntity(
                broadcast,
                categoryName,
                totalViews,
                totalLikes,
                totalSanctions, // 제재 건수 포함
                productList,
                qcardList
        );
    }



    // --- Private Helper Methods (코드 가독성을 위해 분리) ---
    private void saveBroadcastProducts(Long sellerId, Broadcast broadcast, List<BroadcastProductRequest> products) {
        if (products == null || products.isEmpty()) return;

        int order = 1;
        for (BroadcastProductRequest dto : products) {
            // 1. 상품 조회
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            // 2. 소유권 검증 (보안)
            if (!product.getSellerId().equals(sellerId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
            }

            BroadcastProduct bp = BroadcastProduct.builder()
                    .broadcast(broadcast)
                    .productId(dto.getProductId())
                    .bpPrice(dto.getBpPrice())
                    .bpQuantity(dto.getBpQuantity())
                    .displayOrder(order++) // 순서 자동 증가
                    .isPinned(false)
                    .status(BroadcastProductStatus.SELLING)
                    .build();
            broadcastProductRepository.save(bp);
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

        // 2. 다시 저장
        saveBroadcastProducts(sellerId, broadcast, products);
    }

    private void updateQcards(Broadcast broadcast, List<QcardRequest> qcards) {
        qcardRepository.deleteByBroadcast(broadcast);

        // 삭제 후 재저장
        saveQcards(broadcast, qcards);
    }

    private List<BroadcastProductResponse> getProductListResponse(Broadcast broadcast) {
        return broadcast.getProducts().stream()
                .map(bp -> {
                    Product p = productRepository.findById(bp.getProductId()).orElse(null);
                    return BroadcastProductResponse.fromEntity(
                            bp,
                            p != null ? p.getProductName() : "삭제된 상품",
                            p != null ? p.getProductThumbUrl() : "",
                            p != null ? p.getPrice() : 0
                    );
                }).collect(Collectors.toList());
    }

    private List<QcardResponse> getQcardListResponse(Broadcast broadcast) {
        return broadcast.getQcards().stream()
                .map(q -> QcardResponse.builder()
                        .question(q.getQcardQuestion())
                        .sortOrder(q.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }
}
