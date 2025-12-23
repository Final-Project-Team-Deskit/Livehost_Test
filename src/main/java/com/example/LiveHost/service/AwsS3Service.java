package com.example.LiveHost.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.LiveHost.dto.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}") // application.yml에 설정된 버킷명
    private String bucket;

    public ImageUploadResponse uploadFile(MultipartFile file) {
        // 1. 파일명 중복 방지를 위한 UUID 생성 (cat.jpg -> uuid-cat.jpg)
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String storedFileName = UUID.randomUUID().toString() + extension;

        // 2. 메타데이터 설정 (S3에게 파일 크기와 타입을 알려줌)
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try {
            // 3. S3에 파일 업로드 (PublicRead 권한으로 올려야 웹에서 보임)
            amazonS3.putObject(new PutObjectRequest(bucket, storedFileName, file.getInputStream(), metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));

            // 4. 업로드된 파일의 URL 가져오기
            String fileUrl = amazonS3.getUrl(bucket, storedFileName).toString();

            // 5. 응답 DTO 반환
            return ImageUploadResponse.builder()
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .build();

        } catch (IOException e) {
            log.error("S3 파일 업로드 실패", e);
            throw new RuntimeException("이미지 업로드에 실패했습니다.");
        }
    }

    /**
     * S3 파일 삭제
     * @param fileName 저장된 파일명 (URL 아님, uuid-filename.jpg 형태)
     */
    public void deleteFile(String fileName) {
        amazonS3.deleteObject(bucket, fileName);
    }
}
