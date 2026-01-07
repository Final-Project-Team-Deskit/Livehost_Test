package com.example.LiveHost.service;

import com.example.LiveHost.dto.request.LiveDeviceSettingRequest;
import com.example.LiveHost.dto.response.LiveDeviceSettingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LiveDeviceService {

    private final RedisService redisService;

    @Transactional
    public void saveDeviceSettings(Long sellerId, LiveDeviceSettingRequest request) {
        redisService.saveDeviceSettings(sellerId, request);
    }

    @Transactional(readOnly = true)
    public LiveDeviceSettingResponse getDeviceSettings(Long sellerId) {
        return redisService.getDeviceSettings(sellerId);
    }
}
