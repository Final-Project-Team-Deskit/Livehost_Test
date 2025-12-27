package com.example.LiveHost.service;

import com.example.LiveHost.common.enums.BroadcastProductStatus;
import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.enums.VodStatus;
import com.example.LiveHost.common.exception.BusinessException;
import com.example.LiveHost.common.exception.ErrorCode;
import com.example.LiveHost.dto.*;
import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.entity.BroadcastProduct;
import com.example.LiveHost.entity.Qcard;
import com.example.LiveHost.entity.Vod;
import com.example.LiveHost.others.entity.Product;
import com.example.LiveHost.others.entity.TagCategory;
import com.example.LiveHost.others.repository.ProductRepository;
import com.example.LiveHost.others.repository.TagCategoryRepository;
import com.example.LiveHost.repository.BroadcastProductRepository;
import com.example.LiveHost.repository.BroadcastRepository;
import com.example.LiveHost.repository.QcardRepository;
import com.example.LiveHost.repository.VodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final SseService sseService;
    private final OpenViduService openViduService;
    private final SimpMessagingTemplate messagingTemplate;
    private final VodRepository vodRepository;
    private final AwsS3Service s3Service;

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

        // ★ [핵심 추가] 정보가 변경되었음을 시청자들에게 알림 (SSE)
        sseService.notifyBroadcastUpdate(broadcastId);

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
        String categoryName = getCategoryName(broadcast.getTagCategoryId());

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
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        String categoryName = getCategoryName(broadcast.getTagCategoryId());
        List<BroadcastProductResponse> productList = getProductListResponse(broadcast);
        List<QcardResponse> qcardList = getQcardListResponse(broadcast);

        Integer views = 0, likes = 0, sanctions = 0;

        // [수정 완료] LIVE 그룹 (READY, ON_AIR, ENDED)인 경우 Redis 통계 조회
        if (isLiveGroup(broadcast.getStatus())) {
            views = redisService.getStatOrZero(redisService.getViewKey(broadcastId));
            likes = redisService.getStatOrZero(redisService.getLikeKey(broadcastId));
            sanctions = redisService.getStatOrZero(redisService.getSanctionKey(broadcastId));
        }

        return BroadcastResponse.fromEntity(broadcast, categoryName, views, likes, sanctions, productList, qcardList);
    }

    // [Day 5] 방송 목록 조회
    @Transactional(readOnly = true)
    public Object getBroadcastList(Long sellerId, BroadcastSearch condition, Pageable pageable) {

        // 1. 전체(ALL) 탭: 대시보드 형태
        if ("ALL".equalsIgnoreCase(condition.getTab()) || condition.getTab() == null) {
            List<BroadcastListResponse> live = broadcastRepository.findRecentByStatusGroup(sellerId, "LIVE", 1);
            List<BroadcastListResponse> reserved = broadcastRepository.findRecentByStatusGroup(sellerId, "RESERVED", 5);
            List<BroadcastListResponse> vod = broadcastRepository.findRecentByStatusGroup(sellerId, "VOD", 5);

            // 라이브 방송이 있다면 실시간 통계 주입
            if (!live.isEmpty()) fillRealtimeStats(live.get(0));

            return BroadcastAllResponse.builder()
                    .liveBroadcast(live.isEmpty() ? null : live.get(0))
                    .reservedBroadcasts(reserved)
                    .vodBroadcasts(vod)
                    .build();
        }

        // 2. 개별 탭: 리스트 형태 (Slice)
        else {
            Slice<BroadcastListResponse> slice = broadcastRepository.searchBroadcasts(sellerId, condition, pageable);

            // "LIVE" 탭 목록 조회 시에는 각 아이템마다 통계 주입
            if ("LIVE".equalsIgnoreCase(condition.getTab())) {
                slice.getContent().forEach(this::fillRealtimeStats);
            }
            return slice;
        }
    }

    // [Day 6] 방송 시작 (ON_AIR 전환 + 세션 토큰 발급)
    @Transactional
    public String startBroadcast(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSellerId().equals(sellerId)) throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);

        // 상태 변경 (RESERVED/READY -> ON_AIR)
        broadcast.changeStatus(BroadcastStatus.ON_AIR);
        broadcast.setStartedAt(LocalDateTime.now());

        // OpenVidu 세션 생성 및 호스트 토큰 발급
        try {
            // 호스트용 메타데이터
            Map<String, Object> params = Map.of("role", "HOST", "sellerId", sellerId);
            return openViduService.createToken(broadcastId, params);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPENVIDU_ERROR); // 에러코드 정의 필요
        }
    }

    // [Day 6] 방송 종료 (ENDED 전환 + 세션 닫기)
    @Transactional
    public void endBroadcast(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSellerId().equals(sellerId)) throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);

        // 상태 변경
        broadcast.changeStatus(BroadcastStatus.ENDED);
        broadcast.setEndedAt(LocalDateTime.now());

        // OpenVidu 세션 종료
        openViduService.closeSession(broadcastId);

        // SSE로 종료 알림 전송
        sseService.notifyBroadcastUpdate(broadcastId, "BROADCAST_ENDED", "ended");
    }

    // [Day 7] 상품 핀 설정
    @Transactional
    public void pinProduct(Long sellerId, Long broadcastId, Long bpId) {
        // 본인 방송 확인
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));
        if (!broadcast.getSellerId().equals(sellerId)) throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);

        // 1. 기존 핀 해제
        broadcastProductRepository.resetPinByBroadcastId(broadcastId);

        // 2. 새 핀 설정
        BroadcastProduct bp = broadcastProductRepository.findById(bpId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 해당 방송의 상품이 맞는지 더블 체크 (안전장치)
        if (!bp.getBroadcast().getBroadcastId().equals(broadcastId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        bp.setPinned(true);

        // 3. 알림 전송
        sseService.notifyBroadcastUpdate(broadcastId, "PRODUCT_PINNED", bp.getProductId());
    }

    // [Day 7] 시청자 입장 (Connect)
    @EventListener
    public void handleConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        // 1. 프론트엔드에서 보낸 헤더 값 추출
        String broadcastIdStr = accessor.getFirstNativeHeader("broadcastId");

        if (broadcastIdStr != null) {
            Long broadcastId = Long.parseLong(broadcastIdStr);

            // 2. Redis 시청자 수 증가
            redisService.increment(redisService.getViewKey(broadcastId));

            // 3. [핵심] 세션 속성에 broadcastId 저장 (퇴장 시 사용하기 위해)
            // SimpMessageHeaderAccessor를 통해 세션 속성 맵에 접근
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put("broadcastId", broadcastId);
            }

            log.info("User Connected: broadcastId={}", broadcastId);
        }
    }

    // [Day 7] 시청자 퇴장 (Disconnect)
    @EventListener
    public void handleDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        // 1. 세션 속성에서 broadcastId 꺼내기
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("broadcastId")) {
            Long broadcastId = (Long) sessionAttributes.get("broadcastId");

            // 2. Redis 시청자 수 감소
            redisService.decrement(redisService.getViewKey(broadcastId));

            log.info("User Disconnected: broadcastId={}", broadcastId);
        }
    }

    // [Day 7] 좋아요 처리
    public void likeBroadcast(Long broadcastId) {
        redisService.increment(redisService.getLikeKey(broadcastId));
    }

    // OpenVidu Webhook 수신 시 실행
    @Transactional
    public void processVod(OpenViduRecordingWebhook payload) {
        log.info("VOD Processing: {}", payload.getId());

        // 1. 방송 엔티티 조회 (Entity 매핑을 위해 필수)
        Long broadcastId = Long.parseLong(payload.getSessionId());
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        // 2. S3 업로드 (로컬 파일 -> S3)
        // 실제 운영 시엔 파일 경로 로직 구체화 필요 (여기선 예시 URL 사용)
        // String s3Url = s3Service.uploadFile(...);
        String s3Url = "https://your-bucket.s3.ap-northeast-2.amazonaws.com/vods/" + payload.getName() + ".mp4";

        // 3. VOD 엔티티 생성 및 저장
        Vod vod = Vod.builder()
                .broadcast(broadcast) // [수정] broadcastId 대신 broadcast 엔티티 주입
                .vodUrl(s3Url)
                .vodSize(payload.getSize())
                .vodDuration(payload.getDuration().intValue()) // Double -> Integer 형변환 필요 시
                .status(VodStatus.PUBLIC) // 초기값 (공개/비공개 정책에 따름)
                .vodReportCount(0)
                .vodAdminLock(false) // boolean -> DB 'N'
                .build();

        vodRepository.save(vod);
        log.info("VOD Saved: {}", vod.getVodId());
    }

    // [Day 7] 실시간 재고 조회 (방송용 할당 수량)
    @Transactional(readOnly = true)
    public Integer getProductStock(Long broadcastId, Long productId) {
        // BroadcastProductRepository에 findStock 메서드가 있다고 가정
        // (SELECT bp.bpQuantity FROM ...)
        Integer stock = broadcastProductRepository.findStock(broadcastId, productId);

        if (stock == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return stock;
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
    // [핵심] 라이브 그룹인지 판단 (READY, ON_AIR, ENDED)
    private boolean isLiveGroup(BroadcastStatus status) {
        return status == BroadcastStatus.ON_AIR || status == BroadcastStatus.READY || status == BroadcastStatus.ENDED;
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null) return "카테고리 없음";
        return tagCategoryRepository.findById(categoryId)
                .map(TagCategory::getTagCategoryName).orElse("삭제된 카테고리");
    }

    // 리스트 응답(DTO)에 실시간 통계 채워넣기 (Call by Reference)
    private void fillRealtimeStats(BroadcastListResponse response) {
        String viewKey = redisService.getViewKey(response.getBroadcastId());
        String likeKey = redisService.getLikeKey(response.getBroadcastId());
        // BroadcastListResponse DTO에 setter나 builder로 값을 넣을 수 있어야 함
        // response.setTotalViews(redisService.getStatOrZero(viewKey)); // 예시
    }
}
