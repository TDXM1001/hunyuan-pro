package com.hunyuan.sa.bpm.module.integration.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmConnectorDefinitionEntity;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.net.URI;

/**
 * 登记连接器调用、端点校验和幂等重试边界。
 */
@Service
public class BpmConnectorInvocationService {

    @Resource
    private BpmConnectorRegistryService bpmConnectorRegistryService;

    @Resource
    private BpmConnectorReferenceResolver bpmConnectorReferenceResolver;

    @Resource
    private BpmConnectorEndpointPolicy bpmConnectorEndpointPolicy;

    @Resource
    private BpmConnectorTransport bpmConnectorTransport;

    @Resource
    private BpmConnectorCommandStore bpmConnectorCommandStore;

    public JSONObject invokePersistent(String commandKey, Long instanceId, String connectorKey,
                                       Integer connectorVersion, String operationKey, JSONObject payload) {
        JSONObject request = new JSONObject(true);
        request.putAll(payload);
        request.put("idempotencyKey", commandKey);
        BpmCommandRecordEntity existing = bpmConnectorCommandStore.loadOrCreate(
                commandKey, instanceId, request.toJSONString());
        if (existing != null && Integer.valueOf(1).equals(existing.getCommandStatus())) {
            return JSON.parseObject(existing.getResponsePayloadJson());
        }
        BpmCommandRecordEntity command = existing;
        int attempt = (command.getAttemptCount() == null ? 0 : command.getAttemptCount()) + 1;
        try {
            JSONObject response = invoke(connectorKey, connectorVersion, operationKey, request);
            bpmConnectorCommandStore.markSucceeded(command.getCommandRecordId(), attempt, response.toJSONString());
            return response;
        } catch (RuntimeException ex) {
            bpmConnectorCommandStore.markFailed(command.getCommandRecordId(), attempt, limit(ex.getMessage()));
            throw ex;
        }
    }

    public JSONObject invoke(
            String connectorKey,
            Integer connectorVersion,
            String operationKey,
            JSONObject payload
    ) {
        BpmConnectorRegistryService.RegisteredConnector connector =
                bpmConnectorRegistryService.requireOperation(connectorKey, connectorVersion, operationKey);
        BpmConnectorDefinitionEntity definition = connector.definition();
        URI baseEndpoint = URI.create(bpmConnectorReferenceResolver.resolve(definition.getBaseEndpointRef()));
        URI endpoint = baseEndpoint.resolve(connector.operation().path());
        bpmConnectorEndpointPolicy.validate(endpoint);
        String credential = definition.getCredentialRef() == null
                ? null
                : bpmConnectorReferenceResolver.resolve(definition.getCredentialRef());
        int maxAttempts = connector.operation().idempotent() ? readMaxAttempts(definition) : 1;
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return bpmConnectorTransport.invoke(
                        endpoint,
                        connector.operation().method(),
                        payload,
                        credential,
                        definition.getTimeoutMillis()
                );
            } catch (RuntimeException ex) {
                lastFailure = ex;
            }
        }
        throw lastFailure == null ? new IllegalStateException("连接器调用失败") : lastFailure;
    }

    private int readMaxAttempts(BpmConnectorDefinitionEntity definition) {
        try {
            JSONObject policy = JSON.parseObject(definition.getRetryPolicyJson());
            Integer configured = policy == null ? null : policy.getInteger("maxAttempts");
            return configured == null ? 1 : Math.max(1, Math.min(configured, 3));
        } catch (Exception ex) {
            return 1;
        }
    }

    private String limit(String value) {
        if (value == null) return "连接器调用失败";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
