package com.example.LiveHost.controller.seller;

import com.example.LiveHost.common.enums.UploadType;
import com.example.LiveHost.common.exception.ApiResult;
import com.example.LiveHost.dto.response.ImageUploadResponse;
import com.example.LiveHost.service.AwsS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("seller/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final AwsS3Service awsS3Service;

    // 이미지 업로드
    @PostMapping("/{type}")
    public ResponseEntity<ApiResult<ImageUploadResponse>> uploadImage(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable UploadType type,
            @RequestPart("file") MultipartFile file) {

        // sellerId를 같이 넘김
        ImageUploadResponse response = awsS3Service.uploadFile(sellerId, file, type);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    // 이미지 삭제
    @DeleteMapping
    public ResponseEntity<ApiResult<String>> deleteImage(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @RequestParam String fileName) {
        awsS3Service.deleteFile(sellerId, fileName);
        return ResponseEntity.ok(ApiResult.success("이미지가 삭제되었습니다."));
    }
}