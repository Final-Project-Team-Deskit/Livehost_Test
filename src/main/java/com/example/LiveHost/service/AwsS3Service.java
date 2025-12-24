package com.example.LiveHost.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.LiveHost.common.enums.UploadType;
import com.example.LiveHost.common.exception.BusinessException;
import com.example.LiveHost.common.exception.ErrorCode;
import com.example.LiveHost.dto.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 허용 확장자 리스트
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");

    // [이미지 업로드]
        // 기능: 크기, 확장자, 비율 검증 + 판매자별 폴더링
        // 경로: seller_{id}/{type}/{uuid}.{ext}
    public ImageUploadResponse uploadFile(Long sellerId, MultipartFile file, UploadType type) {
        // 1. 빈 파일 체크
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        // 2. 확장자 검증
        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_FILE_EXTENSION);
        }

        // 3. 파일 용량 검증 (Enum에 정의된 크기 기준)
        if (file.getSize() > type.getMaxSizeBytes()) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 4. 이미지 비율 검증 (Enum에 정의된 비율 기준)
        validateImageRatio(file, type);

        // 5. [New] 판매자 ID 기반 폴더 경로 및 파일명 생성
        // 예: seller_100/thumbnail/uuid-image.jpg
        String folderPath = "seller_" + sellerId + "/" + type.name().toLowerCase();
        String storedFileName = folderPath + "/" + UUID.randomUUID() + "." + extension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            // 6. S3 업로드
            amazonS3.putObject(new PutObjectRequest(bucket, storedFileName, inputStream, metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));

            // 7. 결과 반환
            return ImageUploadResponse.builder()
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .fileUrl(amazonS3.getUrl(bucket, storedFileName).toString())
                    .fileSize(file.getSize())
                    .build();

        } catch (IOException e) {
            log.error("S3 업로드 에러", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // [비율 검증 로직] - ImageIO로 이미지를 읽어 가로/세로 비율을 계산 (오차범위 0.05 허용)
    private void validateImageRatio(MultipartFile file, UploadType type) {
        try {
            // InputStream은 한 번 읽으면 소진되지만, MultipartFile.getInputStream()은 호출할 때마다 새 스트림을 반환함
            BufferedImage image = ImageIO.read(file.getInputStream());

            if (image == null) {
                // 이미지가 아닌 파일(손상된 파일 등)일 경우
                throw new BusinessException(ErrorCode.INVALID_FILE_EXTENSION);
            }

            double actualRatio = (double) image.getWidth() / image.getHeight();
            double targetRatio = type.getTargetRatio();

            // 오차 범위 0.05 (약간의 픽셀 오차는 허용)
            if (Math.abs(actualRatio - targetRatio) > 0.05) {
                log.warn("이미지 비율 불일치: 기대={}, 실제={}", targetRatio, actualRatio);
                throw new BusinessException(ErrorCode.INVALID_IMAGE_RATIO);
            }

        } catch (IOException e) {
            log.error("이미지 읽기 및 검증 실패", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // [파일 삭제] - 취소 요청 시 S3에서 파일 제거
    public void deleteFile(Long sellerId, String storedFileName) {
        String expectedPrefix = "seller_" + sellerId + "/";
        if (!storedFileName.startsWith(expectedPrefix)) {
            log.warn("이미지 삭제 권한 없음: 요청자={}, 파일={}", sellerId, storedFileName);
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        try {
            if (amazonS3.doesObjectExist(bucket, storedFileName)) {
                amazonS3.deleteObject(bucket, storedFileName);
            }
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", storedFileName, e);
            throw new BusinessException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    // 확장자 추출 헬퍼 메서드
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            throw new BusinessException(ErrorCode.INVALID_FILE_EXTENSION);
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}