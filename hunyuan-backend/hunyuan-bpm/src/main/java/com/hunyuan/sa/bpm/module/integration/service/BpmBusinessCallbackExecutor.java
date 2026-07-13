package com.hunyuan.sa.bpm.module.integration.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.common.enumeration.BpmCallbackStatusEnum;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * BPM 业务回调统一执行器。
 */
@Service
public class BpmBusinessCallbackExecutor {

    private static final int MAX_RETRY_COUNT = 3;

    private static final int DEFAULT_BATCH_SIZE = 50;

    @Resource
    private BpmCallbackRecordDao bpmCallbackRecordDao;

    @Resource
    private List<BpmBusinessCallbackHandler> callbackHandlers = List.of();

    @Resource
    private BpmConnectorInvocationService bpmConnectorInvocationService;

    public BpmBusinessCallbackExecuteResult execute(Long callbackRecordId, BpmBusinessCallbackTriggerType triggerType) {
        BpmCallbackRecordEntity record = bpmCallbackRecordDao.selectById(callbackRecordId);
        if (record == null) {
            return BpmBusinessCallbackExecuteResult.skipped("回调记录不存在");
        }
        if (BpmCallbackStatusEnum.SUCCEEDED.equalsValue(record.getCallbackStatus())
                || BpmCallbackStatusEnum.COMPENSATED.equalsValue(record.getCallbackStatus())) {
            return BpmBusinessCallbackExecuteResult.skipped("回调记录已处于终态");
        }
        if (BpmCallbackStatusEnum.NEEDS_COMPENSATION.equalsValue(record.getCallbackStatus())) {
            return BpmBusinessCallbackExecuteResult.skipped("回调记录需要人工补偿");
        }

        try {
            if (record.getSubscriptionVersionId() != null) {
                JSONObject payload = JSON.parseObject(record.getRequestPayloadJson());
                payload.put("eventId", record.getEventId());
                payload.put("signatureAlgorithm", "HMAC-SHA256");
                payload.put("signature", sign(record.getSigningSecretRef(), record.getRequestPayloadJson()));
                JSONObject response = bpmConnectorInvocationService.invoke(
                        record.getConnectorKey(), record.getConnectorVersion(), record.getEndpointOperation(), payload
                );
                markSucceeded(record, response == null ? null : response.toJSONString());
                return BpmBusinessCallbackExecuteResult.success();
            }
            BpmBusinessCallbackHandler handler = findHandler(record.getBusinessType());
            if (handler == null) {
                return markFailed(record, "未找到业务回调处理器: " + record.getBusinessType(), null);
            }
            BpmBusinessCallbackResult result = handler.handle(toContext(record));
            if (result != null && result.success()) {
                markSucceeded(record, result.responsePayloadJson());
                return BpmBusinessCallbackExecuteResult.success();
            }
            String failureReason = result == null ? "业务回调处理器返回空结果" : result.failureReason();
            String responsePayloadJson = result == null ? null : result.responsePayloadJson();
            return markFailed(record, failureReason, responsePayloadJson);
        } catch (RuntimeException ex) {
            return markFailed(record, limitFailureReason(ex), ex.getClass().getSimpleName());
        }
    }

    public int executeDueRecords(LocalDateTime now, int batchSize) {
        int limit = batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
        List<BpmCallbackRecordEntity> dueRecords = bpmCallbackRecordDao.selectList(
                Wrappers.<BpmCallbackRecordEntity>lambdaQuery()
                        .and(wrapper -> wrapper
                                .eq(BpmCallbackRecordEntity::getCallbackStatus, BpmCallbackStatusEnum.PENDING.getValue())
                                .or(orWrapper -> orWrapper
                                        .eq(BpmCallbackRecordEntity::getCallbackStatus, BpmCallbackStatusEnum.FAILED.getValue())
                                        .le(BpmCallbackRecordEntity::getNextRetryAt, now)))
                        .orderByAsc(BpmCallbackRecordEntity::getCallbackRecordId)
                        .last("LIMIT " + limit)
        );
        int processed = 0;
        for (BpmCallbackRecordEntity record : dueRecords) {
            BpmBusinessCallbackExecuteResult result = execute(
                    record.getCallbackRecordId(),
                    BpmBusinessCallbackTriggerType.AUTO
            );
            if (result.processed()) {
                processed++;
            }
        }
        return processed;
    }

    private BpmBusinessCallbackHandler findHandler(String businessType) {
        return callbackHandlers.stream()
                .filter(handler -> Objects.equals(handler.businessType(), businessType))
                .findFirst()
                .orElse(null);
    }

    private BpmBusinessCallbackContext toContext(BpmCallbackRecordEntity record) {
        return new BpmBusinessCallbackContext(
                record.getCallbackRecordId(),
                record.getEventId(),
                record.getInstanceId(),
                record.getBusinessType(),
                record.getBusinessId(),
                record.getRequestPayloadJson()
        );
    }

    private void markSucceeded(BpmCallbackRecordEntity record, String responsePayloadJson) {
        BpmCallbackRecordEntity update = new BpmCallbackRecordEntity();
        update.setCallbackStatus(BpmCallbackStatusEnum.SUCCEEDED.getValue());
        update.setResponsePayloadJson(limit(responsePayloadJson, 4000));
        update.setUpdateTime(LocalDateTime.now());
        UpdateWrapper<BpmCallbackRecordEntity> wrapper = new UpdateWrapper<BpmCallbackRecordEntity>()
                .eq("callback_record_id", record.getCallbackRecordId())
                .set("failure_reason", null)
                .set("next_retry_at", null);
        bpmCallbackRecordDao.update(update, wrapper);
    }

    private BpmBusinessCallbackExecuteResult markFailed(
            BpmCallbackRecordEntity record,
            String failureReason,
            String responsePayloadJson
    ) {
        int retryCount = record.getRetryCount() == null ? 1 : record.getRetryCount() + 1;
        BpmCallbackRecordEntity update = new BpmCallbackRecordEntity();
        update.setRetryCount(retryCount);
        update.setFailureReason(limit(StringUtils.hasText(failureReason) ? failureReason : "业务回调执行失败", 1000));
        update.setResponsePayloadJson(limit(responsePayloadJson, 4000));
        update.setUpdateTime(LocalDateTime.now());
        UpdateWrapper<BpmCallbackRecordEntity> wrapper = new UpdateWrapper<BpmCallbackRecordEntity>()
                .eq("callback_record_id", record.getCallbackRecordId());
        if (retryCount >= maxRetryCount(record)) {
            update.setCallbackStatus(BpmCallbackStatusEnum.NEEDS_COMPENSATION.getValue());
            wrapper.set("next_retry_at", null);
        } else {
            update.setCallbackStatus(BpmCallbackStatusEnum.FAILED.getValue());
            update.setNextRetryAt(LocalDateTime.now().plusMinutes(nextBackoffMinutes(retryCount)));
        }
        bpmCallbackRecordDao.update(update, wrapper);
        return BpmBusinessCallbackExecuteResult.failed(update.getFailureReason());
    }

    private int nextBackoffMinutes(int retryCount) {
        if (retryCount <= 1) {
            return 1;
        }
        if (retryCount == 2) {
            return 5;
        }
        return 15;
    }

    private int maxRetryCount(BpmCallbackRecordEntity record) {
        if (!StringUtils.hasText(record.getRetryPolicyJson())) return MAX_RETRY_COUNT;
        try { Integer value = JSON.parseObject(record.getRetryPolicyJson()).getInteger("maxAttempts"); return value == null ? MAX_RETRY_COUNT : Math.max(1, Math.min(value, 10)); }
        catch (Exception ignored) { return MAX_RETRY_COUNT; }
    }

    private String limitFailureReason(RuntimeException ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return ex.getClass().getSimpleName();
        }
        return limit(message, 1000);
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private String sign(String secretRef, String payload) {
        if (!StringUtils.hasText(secretRef) || !secretRef.startsWith("env:")) {
            throw new IllegalStateException("订阅签名密钥引用无效");
        }
        String secret = System.getenv(secretRef.substring(4));
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("订阅签名密钥不可用");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("订阅签名失败", ex);
        }
    }
}
