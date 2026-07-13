package com.hunyuan.sa.bpm.module.runtime.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.common.enumeration.BpmExternalWaitStatusEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmExternalWaitDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTimeEventDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmExternalWaitEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import com.hunyuan.sa.bpm.common.enumeration.BpmTimeEventStatusEnum;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorRegistryService;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorReferenceResolver;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 外部等待的 token、签名、幂等恢复和失败投影。
 */
@Service
public class BpmExternalWaitService {

    private static final int MAX_CALLBACK_BYTES = 1024 * 1024;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Resource
    private BpmExternalWaitDao bpmExternalWaitDao;

    @Resource
    private BpmTimeEventDao bpmTimeEventDao;

    @Resource
    private FlowableProcessInstanceGateway flowableProcessInstanceGateway;

    @Resource
    private BpmConnectorRegistryService bpmConnectorRegistryService;

    @Resource
    private BpmConnectorReferenceResolver bpmConnectorReferenceResolver;

    @Transactional(rollbackFor = Exception.class)
    public PreparedWait prepareWait(
            BpmInstanceEntity instance,
            BpmDefinitionNodeEntity node,
            String engineProcessInstanceId,
            String engineExecutionId,
            String nodeKey,
            String connectorKey,
            String operationKey,
            String timeoutAfter,
            JSONObject requestSnapshot
    ) {
        int attempt = Math.toIntExact(bpmExternalWaitDao.selectCount(
                Wrappers.<BpmExternalWaitEntity>lambdaQuery()
                        .eq(BpmExternalWaitEntity::getInstanceId, instance.getInstanceId())
                        .eq(BpmExternalWaitEntity::getNodeKey, nodeKey)
        ) + 1L);
        String correlationKey = instance.getInstanceId() + ":" + nodeKey + ":" + attempt;
        String token = generateToken();
        Integer connectorVersion = readConnectorVersion(node);
        BpmExternalWaitEntity wait = new BpmExternalWaitEntity();
        wait.setCorrelationKey(correlationKey);
        wait.setCallbackTokenHash(sha256(token));
        wait.setInstanceId(instance.getInstanceId());
        wait.setDefinitionId(instance.getDefinitionId());
        wait.setGraphDefinitionVersionId(instance.getGraphDefinitionVersionId());
        wait.setDefinitionNodeId(node.getDefinitionNodeId());
        wait.setEngineProcessInstanceId(engineProcessInstanceId);
        wait.setEngineExecutionId(engineExecutionId);
        wait.setNodeKey(nodeKey);
        wait.setConnectorKey(connectorKey);
        wait.setConnectorVersion(connectorVersion);
        wait.setOperationKey(operationKey);
        wait.setAttemptNo(attempt);
        wait.setRequestSnapshotJson(requestSnapshot == null ? "{}" : requestSnapshot.toJSONString());
        wait.setWaitStatus(BpmExternalWaitStatusEnum.WAITING.name());
        LocalDateTime timeoutAt = LocalDateTime.now().plus(Duration.parse(timeoutAfter));
        wait.setTimeoutAt(timeoutAt);
        bpmExternalWaitDao.insert(wait);
        BpmTimeEventEntity timeoutEvent = new BpmTimeEventEntity();
        timeoutEvent.setEventKey("WAIT:" + wait.getExternalWaitId() + ":EXTERNAL_TIMEOUT");
        timeoutEvent.setIdempotencyKey(timeoutEvent.getEventKey());
        timeoutEvent.setInstanceId(instance.getInstanceId());
        timeoutEvent.setDefinitionId(instance.getDefinitionId());
        timeoutEvent.setGraphDefinitionVersionId(instance.getGraphDefinitionVersionId());
        timeoutEvent.setDefinitionNodeId(node.getDefinitionNodeId());
        timeoutEvent.setNodeKey(nodeKey);
        timeoutEvent.setEngineProcessInstanceId(engineProcessInstanceId);
        timeoutEvent.setEngineExecutionId(engineExecutionId);
        timeoutEvent.setEventKind("EXTERNAL_TIMEOUT");
        JSONObject timeoutSnapshot = new JSONObject();
        timeoutSnapshot.put("externalWaitId", wait.getExternalWaitId());
        timeoutSnapshot.put("timeoutAfter", timeoutAfter);
        timeoutEvent.setPolicySnapshotJson(timeoutSnapshot.toJSONString());
        timeoutEvent.setScheduledAt(timeoutAt);
        timeoutEvent.setEventStatus(BpmTimeEventStatusEnum.SCHEDULED.name());
        timeoutEvent.setTriggerCount(0);
        bpmTimeEventDao.insert(timeoutEvent);
        return new PreparedWait(token, correlationKey, connectorVersion);
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindExecution(String engineProcessInstanceId, String nodeKey, String engineExecutionId) {
        BpmExternalWaitEntity update = new BpmExternalWaitEntity();
        update.setEngineExecutionId(engineExecutionId);
        int affected = bpmExternalWaitDao.update(update, Wrappers.<BpmExternalWaitEntity>lambdaUpdate()
                .eq(BpmExternalWaitEntity::getEngineProcessInstanceId, engineProcessInstanceId)
                .eq(BpmExternalWaitEntity::getNodeKey, nodeKey)
                .eq(BpmExternalWaitEntity::getWaitStatus, BpmExternalWaitStatusEnum.WAITING.name()));
        if (affected != 1) {
            throw new IllegalStateException("外部等待记录不存在或存在多个活动执行");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean resume(String callbackToken, String appKey, String correlationKey, Integer waitVersion,
                          String signature, String payload) {
        requireCallbackInput(callbackToken, signature, payload);
        String tokenHash = sha256(callbackToken);
        BpmExternalWaitEntity wait = bpmExternalWaitDao.selectOne(
                Wrappers.<BpmExternalWaitEntity>lambdaQuery()
                        .eq(BpmExternalWaitEntity::getCallbackTokenHash, tokenHash)
                        .last("LIMIT 1")
        );
        if (wait == null) {
            throw new IllegalArgumentException("回调令牌无效");
        }
        if (!MessageDigest.isEqual(wait.getConnectorKey().getBytes(StandardCharsets.UTF_8), appKey.getBytes(StandardCharsets.UTF_8))
                || !MessageDigest.isEqual(wait.getCorrelationKey().getBytes(StandardCharsets.UTF_8), correlationKey.getBytes(StandardCharsets.UTF_8))
                || !wait.getAttemptNo().equals(waitVersion)) {
            throw new IllegalArgumentException("回调应用、相关键或等待版本不匹配");
        }
        if (!MessageDigest.isEqual(
                wait.getCallbackTokenHash().getBytes(StandardCharsets.US_ASCII),
                tokenHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("回调令牌无效");
        }
        var connector = bpmConnectorRegistryService.requireOperation(
                wait.getConnectorKey(), wait.getConnectorVersion(), wait.getOperationKey());
        String credentialRef = connector.definition().getCredentialRef();
        if (credentialRef == null || credentialRef.isBlank()) {
            throw new IllegalStateException("回调连接器缺少签名凭据引用");
        }
        String callbackSecret = bpmConnectorReferenceResolver.resolve(credentialRef);
        String expectedSignature = sign(callbackSecret, appKey, correlationKey, waitVersion, payload);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("回调签名校验失败");
        }
        LocalDateTime now = LocalDateTime.now();
        BpmExternalWaitEntity claim = new BpmExternalWaitEntity();
        claim.setWaitStatus(BpmExternalWaitStatusEnum.RESUMED.name());
        claim.setResumedAt(now);
        claim.setCallbackPayloadSnapshotJson(payload);
        int claimed = bpmExternalWaitDao.update(claim, Wrappers.<BpmExternalWaitEntity>lambdaUpdate()
                .eq(BpmExternalWaitEntity::getExternalWaitId, wait.getExternalWaitId())
                .eq(BpmExternalWaitEntity::getWaitStatus, BpmExternalWaitStatusEnum.WAITING.name()));
        if (claimed != 1) {
            return false;
        }
        cancelTimeoutFact(wait.getExternalWaitId(), now);
        try {
            flowableProcessInstanceGateway.trigger(
                    wait.getEngineExecutionId(),
                    "externalCallbackPayload",
                    payload
            );
            return true;
        } catch (Exception ex) {
            BpmExternalWaitEntity failure = new BpmExternalWaitEntity();
            failure.setExternalWaitId(wait.getExternalWaitId());
            failure.setWaitStatus(BpmExternalWaitStatusEnum.FAILED_MANUAL.name());
            failure.setLastError(limitError(ex.getMessage()));
            bpmExternalWaitDao.updateById(failure);
            throw ex;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markTimedOut(Long externalWaitId) {
        BpmExternalWaitEntity timeout = new BpmExternalWaitEntity();
        timeout.setWaitStatus(BpmExternalWaitStatusEnum.TIMED_OUT.name());
        return bpmExternalWaitDao.update(timeout, Wrappers.<BpmExternalWaitEntity>lambdaUpdate()
                .eq(BpmExternalWaitEntity::getExternalWaitId, externalWaitId)
                .eq(BpmExternalWaitEntity::getWaitStatus, BpmExternalWaitStatusEnum.WAITING.name())) == 1;
    }

    private void cancelTimeoutFact(Long externalWaitId, LocalDateTime completedAt) {
        BpmTimeEventEntity cancelled = new BpmTimeEventEntity();
        cancelled.setEventStatus(BpmTimeEventStatusEnum.CANCELLED.name());
        cancelled.setCompletedAt(completedAt);
        bpmTimeEventDao.update(cancelled, Wrappers.<BpmTimeEventEntity>lambdaUpdate()
                .eq(BpmTimeEventEntity::getEventKey, "WAIT:" + externalWaitId + ":EXTERNAL_TIMEOUT")
                .in(BpmTimeEventEntity::getEventStatus,
                        BpmTimeEventStatusEnum.SCHEDULED.name(),
                        BpmTimeEventStatusEnum.FAILED_RETRYABLE.name()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPendingForInstance(Long instanceId) {
        BpmExternalWaitEntity update = new BpmExternalWaitEntity();
        update.setWaitStatus(BpmExternalWaitStatusEnum.CANCELLED.name());
        update.setCancelledAt(LocalDateTime.now());
        bpmExternalWaitDao.update(update, Wrappers.<BpmExternalWaitEntity>lambdaUpdate()
                .eq(BpmExternalWaitEntity::getInstanceId, instanceId)
                .in(BpmExternalWaitEntity::getWaitStatus,
                        BpmExternalWaitStatusEnum.WAITING.name(),
                        BpmExternalWaitStatusEnum.FAILED_MANUAL.name()));
    }

    private void requireCallbackInput(String token, String signature, String payload) {
        if (token == null || token.length() < 12 || token.length() > 256) {
            throw new IllegalArgumentException("回调令牌无效");
        }
        if (signature == null || signature.length() != 64) {
            throw new IllegalArgumentException("回调签名校验失败");
        }
        if (payload == null || payload.getBytes(StandardCharsets.UTF_8).length > MAX_CALLBACK_BYTES) {
            throw new IllegalArgumentException("回调载荷为空或超过限制");
        }
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("运行环境缺少 SHA-256", ex);
        }
    }

    public static String sign(String secret, String appKey, String correlationKey, Integer waitVersion, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String canonical = appKey + "\n" + correlationKey + "\n" + waitVersion + "\n" + payload;
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("生成回调签名失败", ex);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Integer readConnectorVersion(BpmDefinitionNodeEntity node) {
        try {
            Integer version = JSON.parseObject(node.getCompiledNodeSnapshotJson()).getInteger("connectorVersion");
            return version == null ? 1 : version;
        } catch (Exception ex) {
            return 1;
        }
    }

    private String limitError(String message) {
        if (message == null) {
            return "恢复外部等待失败";
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }

    public record PreparedWait(String callbackToken, String correlationKey, Integer connectorVersion) {
    }
}
