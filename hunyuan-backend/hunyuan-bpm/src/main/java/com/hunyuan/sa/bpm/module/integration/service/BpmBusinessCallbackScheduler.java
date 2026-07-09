package com.hunyuan.sa.bpm.module.integration.service;

import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * BPM 业务回调自动重试扫描器。
 */
@Component
public class BpmBusinessCallbackScheduler {

    private static final int BATCH_SIZE = 50;

    @Resource
    private BpmBusinessCallbackExecutor callbackExecutor;

    @Scheduled(fixedDelay = 60_000L)
    public void scanDueCallbackRecords() {
        callbackExecutor.executeDueRecords(LocalDateTime.now(), BATCH_SIZE);
    }
}
