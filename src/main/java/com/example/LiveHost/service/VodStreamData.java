package com.example.LiveHost.service;

import java.io.InputStream;

public record VodStreamData(
        InputStream stream,
        long contentLength,
        long totalLength,
        long rangeStart,
        long rangeEnd,
        String contentType
) {
}
