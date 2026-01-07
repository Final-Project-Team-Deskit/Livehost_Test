package com.example.LiveHost.controller;

import com.example.LiveHost.service.VodService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vods")
@RequiredArgsConstructor
public class VodController {

    private final VodService vodService;

    @GetMapping("/{vodId}/stream")
    public ResponseEntity<InputStreamResource> streamVod(
            @PathVariable Long vodId,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) {
        return vodService.streamVod(vodId, rangeHeader);
    }
}
