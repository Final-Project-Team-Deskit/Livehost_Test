package com.example.LiveHost.service;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.enums.VodStatus;
import com.example.LiveHost.common.exception.BusinessException;
import com.example.LiveHost.common.exception.ErrorCode;
import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.entity.Vod;
import com.example.LiveHost.repository.BroadcastRepository;
import com.example.LiveHost.repository.VodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VodService {

    private final BroadcastRepository broadcastRepository;
    private final VodRepository vodRepository;
    private final AwsS3Service s3Service;

    @Transactional(readOnly = true)
    public VodStreamData getVodStream(Long broadcastId, String rangeHeader) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (broadcast.getStatus() == BroadcastStatus.DELETED || broadcast.getStatus() == BroadcastStatus.CANCELED) {
            throw new BusinessException(ErrorCode.BROADCAST_NOT_FOUND);
        }

        Vod vod = vodRepository.findByBroadcast(broadcast)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOD_NOT_FOUND));

        if (vod.getStatus() != VodStatus.PUBLIC) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        if (vod.getVodUrl() == null || vod.getVodUrl().isBlank()) {
            throw new BusinessException(ErrorCode.VOD_NOT_FOUND);
        }

        long totalLength = s3Service.getObjectSize(vod.getVodUrl());
        if (totalLength <= 0) {
            throw new BusinessException(ErrorCode.VOD_NOT_FOUND);
        }
        long start = 0;
        long end = totalLength > 0 ? totalLength - 1 : 0;

        if (rangeHeader != null && !rangeHeader.isBlank()) {
            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            if (ranges.isEmpty()) {
                throw new IllegalArgumentException("Invalid Range Header");
            }
            HttpRange range = ranges.get(0);
            start = range.getRangeStart(totalLength);
            end = range.getRangeEnd(totalLength);
            if (start >= totalLength) {
                throw new IllegalArgumentException("Range Not Satisfiable");
            }
        }

        return s3Service.getVodStream(vod.getVodUrl(), start, end);
    }
}
