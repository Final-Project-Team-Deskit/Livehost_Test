package com.example.LiveHost.service;

import com.example.LiveHost.common.enums.BroadcastProductStatus;
import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.enums.SanctionType;
import com.example.LiveHost.common.enums.VodStatus;
import com.example.LiveHost.common.exception.BusinessException;
import com.example.LiveHost.common.exception.ErrorCode;
import com.example.LiveHost.dto.*;
import com.example.LiveHost.entity.*;
import com.example.LiveHost.others.entity.Product;
import com.example.LiveHost.others.entity.Seller;
import com.example.LiveHost.others.entity.TagCategory;
import com.example.LiveHost.others.repository.ProductRepository;
import com.example.LiveHost.others.repository.SellerRepository;
import com.example.LiveHost.others.repository.TagCategoryRepository;
import com.example.LiveHost.repository.*;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import javax.net.ssl.*;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.LiveHost.entity.QBroadcast.broadcast;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastService {

    private final BroadcastRepository broadcastRepository;
    private final BroadcastProductRepository broadcastProductRepository;
    private final QcardRepository qcardRepository;
    private final BroadcastResultRepository broadcastResultRepository;
    private final VodRepository vodRepository;

    private final SellerRepository sellerRepository;
    private final TagCategoryRepository tagCategoryRepository;
    private final ProductRepository productRepository;
    private final SanctionRepository sanctionRepository;
    private final ViewHistoryRepository viewHistoryRepository;

    private final RedisService redisService;
    private final SseService sseService;
    private final OpenViduService openViduService;
    private final AwsS3Service s3Service;

    @Value("${openvidu.url}")
    private String OPENVIDU_URL;

    @Value("${openvidu.secret}")
    private String OPENVIDU_SECRET;


    // =====================================================================
    // 1. [판매자] 방송 예약 (Redis Lock + 슬롯 제한 + 기존 로직 통합)
    // =====================================================================
    @Transactional
    public Long createBroadcast(Long sellerId, BroadcastCreateRequest request) {
        String lockKey = "lock:seller:" + sellerId + ":broadcast_create";

        if (!redisService.acquireLock(lockKey, 3000)) throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);

        try {
            long reservedCount = broadcastRepository.countBySellerIdAndStatus(sellerId, BroadcastStatus.RESERVED);
            if (reservedCount >= 7) throw new BusinessException(ErrorCode.RESERVATION_LIMIT_EXCEEDED);

            long slotCount = broadcastRepository.countByTimeSlot(request.getScheduledAt(), request.getScheduledAt().plusHours(1));
            if (slotCount >= 3) throw new BusinessException(ErrorCode.BROADCAST_SLOT_FULL);

            Seller seller = sellerRepository.findById(sellerId).orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
            TagCategory category = tagCategoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

            Broadcast broadcast = Broadcast.builder()
                    .seller(seller)
                    .tagCategory(category)
                    .broadcastTitle(request.getTitle())
                    .broadcastNotice(request.getNotice())
                    .scheduledAt(request.getScheduledAt())
                    .broadcastLayout(request.getBroadcastLayout())
                    .broadcastThumbUrl(request.getThumbnailUrl())
                    .broadcastWaitUrl(request.getWaitScreenUrl())
                    .status(BroadcastStatus.RESERVED)
                    .build();

            Broadcast saved = broadcastRepository.save(broadcast);
            saveBroadcastProducts(sellerId, saved, request.getProducts());
            saveQcards(saved, request.getQcards());

            log.info("방송 생성 완료: id={}", saved.getBroadcastId());
            return saved.getBroadcastId();
        } finally {
            redisService.releaseLock(lockKey);
        }
    }

    // =====================================================================
    // 2. [판매자] 방송 수정
    // =====================================================================
    @Transactional
    public Long updateBroadcast(Long sellerId, Long broadcastId, BroadcastUpdateRequest request) {
        // 1. 방송 조회
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        // 2. 권한 검증 (Aspect가 해주지만, Service에서도 이중 체크 권장)
        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 3. 카테고리 검증
        TagCategory category = tagCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 4. 상태 검증 분기
        if (broadcast.getStatus() == BroadcastStatus.RESERVED || broadcast.getStatus() == BroadcastStatus.CANCELED) {
            broadcast.updateBroadcastInfo(
                    category, request.getTitle(), request.getNotice(),
                    request.getScheduledAt(), request.getThumbnailUrl(),
                    request.getWaitScreenUrl(), request.getBroadcastLayout()
            );
            updateBroadcastProducts(sellerId, broadcast, request.getProducts());
            updateQcards(broadcast, request.getQcards());
        } else {
            broadcast.updateLiveBroadcastInfo(
                    category, request.getTitle(), request.getNotice(),
                    request.getThumbnailUrl(), request.getWaitScreenUrl()
            );
            sseService.notifyBroadcastUpdate(broadcastId); // SSE 알림
        }

        return broadcast.getBroadcastId();

        // 트랜잭션이 닫힐 때, JPA가 값이 달라진 것을 감지하고, 자동으로 UPDATE 쿼리를 생성해서 DB에 날림
    }

    // =====================================================================
    // 3. [판매자] 방송 예약 취소 (DELETE)
    // =====================================================================
    @Transactional
    public void cancelBroadcast(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        // 권한 체크
        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 상태 체크 (예약 상태일 때만 삭제 가능)
        if (broadcast.getStatus() != BroadcastStatus.RESERVED && broadcast.getStatus() != BroadcastStatus.CANCELED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 상태를 DELETED로 변경
        broadcast.deleteBroadcast();
        log.info("방송 삭제(취소) 처리 완료: id={}, status={}", broadcastId, broadcast.getStatus());
        // 트랜잭션이 닫힐 때, JPA가 값이 달라진 것을 감지하고, 자동으로 UPDATE 쿼리를 생성해서 DB에 날림
    }

    // =====================================================================
    //  4. [판매자] 내 상품 목록 조회 (방송 등록 화면 팝업용)
    // =====================================================================
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

    // =====================================================================
    // 5. [조회] 방송 상세 (Detail) - 판매자 vs 시청자 분리
    // =====================================================================

    // 5-1. [판매자] 상세 조회 (권한 체크 O, 모든 상태 조회 가능)
    @Transactional(readOnly = true)
    public BroadcastResponse getBroadcastDetail(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        // 본인 확인
        if (!broadcast.getSeller().getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return createBroadcastResponse(broadcast);
    }

    // 5-2. [시청자] 상세 조회 (권한 체크 X, 공개 상태 체크 O)
    @Transactional(readOnly = true)
    public BroadcastResponse getPublicBroadcastDetail(Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        // [보안] 취소되거나 삭제된 방송은 조회 불가
        if (broadcast.getStatus() == BroadcastStatus.DELETED || broadcast.getStatus() == BroadcastStatus.CANCELED) {
            throw new BusinessException(ErrorCode.BROADCAST_NOT_FOUND);
        }

        // [보안] 비공개 VOD 조회 불가
        if (broadcast.getStatus() == BroadcastStatus.VOD) {
            Vod vod = vodRepository.findByBroadcast(broadcast) // OneToOne
                    .orElseThrow(() -> new BusinessException(ErrorCode.VOD_NOT_FOUND));
            if (vod.getStatus() != VodStatus.PUBLIC) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
            }
        }

        return createBroadcastResponse(broadcast);
    }

    // 5-3. 실시간 통계 조회 (Polling용 - 가벼운 API)
    @Transactional(readOnly = true)
    public BroadcastStatsResponse getBroadcastStats(Long broadcastId) {
        // 방송 존재 여부 체크
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        // [보안] 취소되거나 삭제된 방송은 조회 불가
        if (broadcast.getStatus() == BroadcastStatus.DELETED || broadcast.getStatus() == BroadcastStatus.CANCELED) {
            throw new BusinessException(ErrorCode.BROADCAST_NOT_FOUND);
        }

        int views = 0;
        int likes = 0;
        int reports = 0;

        if (isLiveGroup(broadcast.getStatus())) {
            // 라이브 중: Redis 조회 (빠름)
            views = redisService.getRealtimeViewerCount(broadcastId);
            likes = redisService.getLikeCount(broadcastId);
            reports = redisService.getReportCount(broadcastId);
        } else {
            // 종료됨: DB 결과 조회
            BroadcastResult result = broadcastResultRepository.findById(broadcastId).orElse(null);
            if (result != null) {
                views = result.getTotalViews();
                likes = result.getTotalLikes();
                reports = sanctionRepository.countByBroadcast(broadcast);
            }
        }

        return BroadcastStatsResponse.builder()
                .viewerCount(views)
                .likeCount(likes)
                .reportCount(reports)
                .build();
    }


    // =====================================================================
    // 6. [조회] 방송 목록 (List/Overview)
    // =====================================================================
    // 6-1. [공용] 방송 목록 조회
    @Transactional(readOnly = true)
    public Object getPublicBroadcasts(BroadcastSearch condition, Pageable pageable) {
        if ("ALL".equalsIgnoreCase(condition.getTab())) return getOverview(null, false);
        Slice<BroadcastListResponse> list = broadcastRepository.searchBroadcasts(null, condition, pageable, false);
        injectLiveStats(list.getContent());
        return list;
    }
    // 6-2. [판매자] 방송 목록 조회
    @Transactional(readOnly = true)
    public Object getSellerBroadcasts(Long sellerId, BroadcastSearch condition, Pageable pageable) {
        if ("ALL".equalsIgnoreCase(condition.getTab())) return getOverview(sellerId, true);
        Slice<BroadcastListResponse> list = broadcastRepository.searchBroadcasts(sellerId, condition, pageable, true);
        injectLiveStats(list.getContent());
        return list;
    }

    // =====================================================================
    // 7. [제어] 방송 시작 / 입장 / 종료 (Start/Join/End)
    // =====================================================================
    // 7-1. [판매자] 방송 시작 (Start) -> ON_AIR 전환, HOST 토큰 발급
    @Transactional
    public String startBroadcast(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSeller().getSellerId().equals(sellerId)) throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);

        // 상태 변경 (RESERVED/READY -> ON_AIR)
        broadcast.startBroadcast("session-" + broadcastId);

        // OpenVidu 세션 생성 및 호스트 토큰 발급
        try {
            // 호스트용 메타데이터
            Map<String, Object> params = Map.of("role", "HOST", "sellerId", sellerId);
            return openViduService.createToken(broadcastId, params);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPENVIDU_ERROR); // 에러코드 정의 필요
        }
    }

    // 7-2. [시청자] 방송 입장 (Join) -> SUBSCRIBER 토큰 발급
    public String joinBroadcast(Long broadcastId, String viewerId){
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (broadcast.getStatus() == BroadcastStatus.STOPPED) {
            throw new BusinessException(ErrorCode.BROADCAST_STOPPED_BY_ADMIN);
        }
        if (!isLiveGroup(broadcast.getStatus())) {
            throw new BusinessException(ErrorCode.BROADCAST_NOT_ON_AIR);
        }

        // UV 집계 (Redis) - 실제 입장은 WebSocket Connect에서 처리되지만 토큰 발급 시에도 기록 가능
        String uuid = (viewerId != null) ? viewerId : UUID.randomUUID().toString();
        redisService.enterLiveRoom(broadcastId, uuid); // 토큰 발급 시점에도 기록

        // [핵심] 시청자용 토큰 발급
        try {
            Map<String, Object> params = Map.of("role", "SUBSCRIBER");
            return openViduService.createToken(broadcastId, params);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPENVIDU_ERROR);
        }
    }

    // [Day 6] 방송 종료 (ENDED 전환 + 세션 닫기)
    @Transactional
    public void endBroadcast(Long sellerId, Long broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!broadcast.getSeller().getSellerId().equals(sellerId)) throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);

        // 상태 변경
        broadcast.endBroadcast();

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
        if (!broadcast.getSeller().getSellerId().equals(sellerId)) throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);

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

    // WebSocket Event 시청자 집계  (Connect)
    @EventListener
    public void handleConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String bId = accessor.getFirstNativeHeader("broadcastId");
        String vId = accessor.getFirstNativeHeader("X-Viewer-Id");
        if (bId != null && vId != null) {
            redisService.enterLiveRoom(Long.parseLong(bId), vId);
            Map<String, Object> attrs = accessor.getSessionAttributes();
            if (attrs != null) { attrs.put("broadcastId", bId); attrs.put("viewerId", vId); }
        }
    }

    // WebSocket Event 시청자 집계 (Disconnect)
    @EventListener
    public void handleDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null && attrs.containsKey("broadcastId")) {
            redisService.exitLiveRoom(Long.parseLong((String)attrs.get("broadcastId")), (String)attrs.get("viewerId"));
        }
    }

    // [Day 7] 좋아요 처리
    public void likeBroadcast(Long broadcastId, Long memberId) {
        redisService.toggleLike(broadcastId, memberId);
    }

    // =====================================================================
    // [Webhook] VOD 스트리밍 업로드 & 결과 확정 (비동기 처리)
    // =====================================================================
    @Transactional
    public void processVod(OpenViduRecordingWebhook payload) {
        // 1. 방송 조회
        Long broadcastId = Long.parseLong(payload.getSessionId().replace("broadcast-", ""));
        Broadcast broadcast = broadcastRepository.findById(broadcastId).orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        String recordingId = payload.getId();
        String s3Key = "seller_" + broadcast.getSeller().getSellerId() + "/vods/" + recordingId + ".mp4";
        String s3Url = "";

        // 2. OpenVidu 서버에서 영상 다운로드 -> S3 업로드 (스트리밍)
        try {
            disableSslVerification(); // SSL 인증서 무시 (개발용)

            // URL 생성 (LiveHostController 로직 참조)
            String videoUrl = OPENVIDU_URL.replaceAll("/$", "") +
                    "/openvidu/recordings/" + recordingId + "/" + recordingId + ".mp4";

            URL url = new URL(videoUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Basic Auth 헤더 설정
            String auth = "OPENVIDUAPP:" + OPENVIDU_SECRET;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = conn.getInputStream()) {
                    // [핵심] S3Service의 스트리밍 업로드 호출
                    long contentLength = conn.getContentLengthLong();
                    s3Url = s3Service.uploadVodStream(inputStream, s3Key, contentLength);
                    log.info("VOD Upload Success: {}", s3Url);
                }
            } else {
                log.error("Failed to fetch recording from OpenVidu: {}", responseCode);
            }
        } catch (Exception e) {
            log.error("VOD Processing Error", e);
            // 업로드 실패해도 통계 저장을 위해 로직 계속 진행 (URL은 빈 값 or 에러처리)
        }

        // 3. VOD 정보 DB 저장
        boolean isStopped = broadcast.getStatus() == BroadcastStatus.STOPPED;
        VodStatus status = isStopped ? VodStatus.PRIVATE : VodStatus.PUBLIC;

        Vod vod = Vod.builder()
                .broadcast(broadcast)
                .vodUrl(s3Url) // S3 URL 저장
                .vodSize(payload.getSize())
                .vodDuration(payload.getDuration().intValue())
                .status(status)
                .vodReportCount(0)
                .vodAdminLock(isStopped)
                .build();
        vodRepository.save(vod);

        // 4. 통계 확정 (Redis -> DB)
        int uv = redisService.getTotalUniqueViewerCount(broadcastId);
        int likes = redisService.getLikeCount(broadcastId);
        int reports = redisService.getReportCount(broadcastId);
        int mv = redisService.getMaxViewers(broadcastId);
        LocalDateTime peak = redisService.getMaxViewersTime(broadcastId);
        Double avg = viewHistoryRepository.getAverageWatchTime(broadcastId);

        BroadcastResult result = BroadcastResult.builder()
                .broadcast(broadcast)
                .totalViews(uv)
                .totalLikes(likes)
                .totalReports(reports)
                .avgWatchTime(avg != null ? avg.intValue() : 0)
                .maxViews(mv)
                .pickViewsAt(peak)
                .totalReports(reports)
                .totalChats(0)              // TODO: Chatting Service 연동 필요 (현재 0)
                .totalSales(BigDecimal.ZERO)// TODO: Order Service 연동 필요 (현재 0)
                .build();
        broadcastResultRepository.save(result);

        // 5. 리소스 정리
        redisService.deleteBroadcastKeys(broadcastId);
        if (isStopped || broadcast.getStatus() == BroadcastStatus.ENDED) {
            broadcast.changeStatus(BroadcastStatus.VOD);
        }
    }

    // [Day 7] 실시간 재고 조회 (방송용 할당 수량)
    @Transactional(readOnly = true)
    public Integer getProductStock(Long broadcastId, Long productId) {
        Integer stock = broadcastProductRepository.findStock(broadcastId, productId);

        if (stock == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return stock;
    }

    //  신고 (1인 1회)
    public void reportBroadcast(Long broadcastId, Long memberId) {
        if (!broadcastRepository.existsById(broadcastId)) throw new BusinessException(ErrorCode.BROADCAST_NOT_FOUND);
        redisService.reportBroadcast(broadcastId, memberId);
    }

    // 결과 리포트 조회 (화면 정의서 대응)
    @Transactional(readOnly = true)
    public BroadcastResultResponse getBroadcastResult(Long broadcastId, Long requesterId, boolean isAdmin) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (!isAdmin && !broadcast.getSeller().getSellerId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        BroadcastResult r = broadcastResultRepository.findById(broadcastId).orElse(null);
        Vod vod = vodRepository.findByBroadcast(broadcast).orElse(null);

        // 통계 데이터 매핑 (Null Safe)
        int views = 0, likes = 0, chats = 0, maxV = 0, reports = 0, sanctions = 0;
        long avgTime = 0;
        BigDecimal sales = BigDecimal.ZERO;
        LocalDateTime maxTime = null;

        if (r != null) {
            views = r.getTotalViews(); likes = r.getTotalLikes(); sales = r.getTotalSales();
            chats = r.getTotalChats(); maxV = r.getMaxViews(); maxTime = r.getPickViewsAt();
            avgTime = r.getAvgWatchTime(); reports = r.getTotalReports();
        }
        sanctions = sanctionRepository.countByBroadcast(broadcast);

        // 상품 성과 리스트
        List<BroadcastResultResponse.ProductSalesStat> productStats = broadcast.getProducts().stream()
                .map(bp -> {
                    Product p = productRepository.findById(bp.getProductId()).orElse(null);
                    return BroadcastResultResponse.ProductSalesStat.builder()
                            .productId(bp.getProductId())
                            .productName(p != null ? p.getProductName() : "")
                            .imageUrl(p != null ? p.getProductThumbUrl() : "")
                            .salesAmount(BigDecimal.ZERO) // TODO: OrderService 연동
                            .price(bp.getBpPrice())
                            .build();
                }).collect(Collectors.toList());

        // 방송 시간 계산
        long duration = 0;
        if (broadcast.getStartedAt() != null && broadcast.getEndedAt() != null) {
            duration = java.time.Duration.between(broadcast.getStartedAt(), broadcast.getEndedAt()).toMinutes();
        }

        return BroadcastResultResponse.builder()
                .broadcastId(broadcast.getBroadcastId())
                .title(broadcast.getBroadcastTitle())
                .startAt(broadcast.getStartedAt())
                .endAt(broadcast.getEndedAt())
                .durationMinutes(duration)
                .status(broadcast.getStatus())
                .stoppedReason(broadcast.getBroadcastStoppedReason())
                .totalViews(views).totalLikes(likes).totalSales(sales)
                .totalChats(chats).maxViewers(maxV).maxViewerTime(maxTime).avgWatchTime(avgTime)
                .reportCount(reports).sanctionCount(sanctions)
                .vodUrl((vod != null && vod.getStatus() != VodStatus.DELETED) ? vod.getVodUrl() : null)
                .vodStatus(vod != null ? vod.getStatus() : null)
                .isEncoding(vod == null)
                .productStats(productStats)
                .build();
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(Long sellerId, String period) {
        var sales = broadcastResultRepository.getSalesChart(sellerId, period);
        var arpu = broadcastResultRepository.getArpuChart(sellerId, period);
        List<StatisticsResponse.BroadcastRank> best, worst, topView;

        if (sellerId != null) { // 판매자
            best = broadcastResultRepository.getRanking(sellerId, period,"SALES", true, 5);
            worst = broadcastResultRepository.getRanking(sellerId, period,"SALES", false, 5);
            topView = broadcastResultRepository.getRanking(sellerId, period,"VIEWS", true, 5);
        } else { // 관리자
            best = broadcastResultRepository.getRanking(null, period,"SALES", true, 10);
            worst = broadcastResultRepository.getRanking(null, period,"SALES", false, 10);
            topView = List.of();
        }
        // ARPU 단일값 계산 (여기선 생략, 차트만 반환)
        return StatisticsResponse.builder()
                .salesChart(sales).arpuChart(arpu)
                .bestBroadcasts(best).worstBroadcasts(worst).topViewerBroadcasts(topView)
                .build();
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

    private BroadcastResponse createBroadcastResponse(Broadcast broadcast) {
        Integer views = 0, likes = 0, reports = 0;
        if (isLiveGroup(broadcast.getStatus())) {
            views = redisService.getRealtimeViewerCount(broadcast.getBroadcastId());
            likes = redisService.getLikeCount(broadcast.getBroadcastId());
            reports = redisService.getReportCount(broadcast.getBroadcastId());
        } else {
            BroadcastResult result = broadcastResultRepository.findById(broadcast.getBroadcastId()).orElse(null);
            if (result != null) { views = result.getTotalViews(); likes = result.getTotalLikes(); }
            reports = sanctionRepository.countByBroadcast(broadcast);
        }
        return BroadcastResponse.fromEntity(broadcast, broadcast.getTagCategory().getTagCategoryName(), views, likes, reports, getProductListResponse(broadcast), getQcardListResponse(broadcast));
    }

    private BroadcastAllResponse getOverview(Long sellerId, boolean isAdmin) {
        List<BroadcastListResponse> onAir = broadcastRepository.findTop5ByStatus(sellerId, List.of(BroadcastStatus.ON_AIR, BroadcastStatus.READY), new OrderSpecifier<>(Order.DESC, broadcast.startedAt), isAdmin);
        List<BroadcastListResponse> reserved = broadcastRepository.findTop5ByStatus(sellerId, List.of(BroadcastStatus.RESERVED), new OrderSpecifier<>(Order.ASC, broadcast.scheduledAt), isAdmin);
        List<BroadcastListResponse> vod = broadcastRepository.findTop5ByStatus(sellerId, List.of(BroadcastStatus.VOD, BroadcastStatus.ENDED, BroadcastStatus.STOPPED), new OrderSpecifier<>(Order.DESC, broadcast.endedAt), isAdmin);
        injectLiveDetails(onAir);
        return BroadcastAllResponse.builder().onAir(onAir).reserved(reserved).vod(vod).build();
    }

    private void injectLiveStats(List<BroadcastListResponse> list) {
        list.forEach(item -> {
            if (item.getStatus() == BroadcastStatus.ON_AIR) {
                // Redis 실시간 통계
                item.setLiveViewerCount(redisService.getRealtimeViewerCount(item.getBroadcastId()));
                item.setTotalLikes(redisService.getLikeCount(item.getBroadcastId()));
                item.setReportCount(redisService.getReportCount(item.getBroadcastId()));
            }
        });
    }

    private void injectLiveDetails(List<BroadcastListResponse> list) {
        list.forEach(item -> {
            if (item.getStatus() == BroadcastStatus.ON_AIR) {
                // Redis 실시간 통계
                item.setLiveViewerCount(redisService.getRealtimeViewerCount(item.getBroadcastId()));
                item.setTotalLikes(redisService.getLikeCount(item.getBroadcastId()));
                item.setReportCount(redisService.getReportCount(item.getBroadcastId()));

                // [추가] 상품 재고 리스트 주입
                Broadcast b = broadcastRepository.findById(item.getBroadcastId()).orElse(null);
                if (b != null) {
                    item.setProducts(b.getProducts().stream().map(bp -> {
                        Product p = productRepository.findById(bp.getProductId()).orElse(null);
                        return BroadcastListResponse.SimpleProductInfo.builder()
                                .name(p!=null ? p.getProductName() : "")
                                .stock(p!=null ? p.getStockQty() : 0)
                                .isSoldOut(p!=null && p.getStockQty()<=0).build();
                    }).collect(Collectors.toList()));
                }
            }
        });
    }

    // SSL 무시 (LiveHostController 로직 복사)
    private void disableSslVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            } };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
