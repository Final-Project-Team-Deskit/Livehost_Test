package com.example.LiveHost.controller;

import com.example.LiveHost.dto.ImageUploadResponse;
import com.example.LiveHost.service.AwsS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final AwsS3Service awsS3Service;

    @PostMapping
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestPart("file") MultipartFile file) {
        ImageUploadResponse response = awsS3Service.uploadFile(file);
        return ResponseEntity.ok(response);
    }

    /**
     * 이미지 삭제 API (취소 버튼 클릭 시 호출)
     * URL: DELETE /api/v1/uploads?fileName=uuid-cat.jpg
     */
    @DeleteMapping
    public ResponseEntity<String> deleteImage(@RequestParam String fileName) {
        awsS3Service.deleteFile(fileName);
        return ResponseEntity.ok("이미지가 삭제되었습니다.");
    }
}