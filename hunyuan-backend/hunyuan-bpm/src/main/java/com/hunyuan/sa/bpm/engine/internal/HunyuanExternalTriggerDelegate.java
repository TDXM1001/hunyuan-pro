package com.hunyuan.sa.bpm.engine.internal;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorInvocationService;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmGraphRuntimeMetadataService;
import jakarta.annotation.Resource;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Flowable 外部触发节点的固定 Hunyuan 入口。
 */
@Component("hunyuanExternalTriggerDelegate")
public class HunyuanExternalTriggerDelegate implements JavaDelegate {

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @Resource
    private BpmConnectorInvocationService bpmConnectorInvocationService;

    @Resource
    private BpmExternalWaitService bpmExternalWaitService;

    @Resource
    private BpmGraphRuntimeMetadataService bpmGraphRuntimeMetadataService;

    private Expression externalNodeKey;
    private Expression connectorKey;
    private Expression operationKey;
    private Expression waitMode;

    @Override
    public void execute(DelegateExecution execution) {
        Long instanceId = requireInstanceId(execution);
        BpmInstanceEntity instance = bpmInstanceDao.selectById(instanceId);
        if (instance == null) {
            throw new IllegalStateException("外部触发节点对应的 Hunyuan 实例不存在");
        }
        String nodeKey = expressionValue(externalNodeKey, execution);
        String connector = expressionValue(connectorKey, execution);
        String operation = expressionValue(operationKey, execution);
        String mode = expressionValue(waitMode, execution);
        BpmDefinitionNodeEntity node = runtimeNodeSnapshot(instance, nodeKey);
        if (node == null) {
            throw new IllegalStateException("外部触发节点冻结快照不存在");
        }
        JSONObject snapshot = JSON.parseObject(node.getCompiledNodeSnapshotJson());
        JSONObject request = mapRequest(instance.getCurrentFormDataSnapshotJson(), snapshot.getJSONObject("requestMapping"));
        Integer version = snapshot.getInteger("connectorVersion");
        if ("WAIT_CALLBACK".equals(mode)) {
            JSONObject timeoutPolicy = snapshot.getJSONObject("timeoutPolicy");
            BpmExternalWaitService.PreparedWait prepared = bpmExternalWaitService.prepareWait(
                    instance,
                    node,
                    execution.getProcessInstanceId(),
                    execution.getId(),
                    nodeKey,
                    connector,
                    operation,
                    timeoutPolicy.getString("timeoutAfter"),
                    request
            );
            version = prepared.connectorVersion();
            request.put("callbackToken", prepared.callbackToken());
            request.put("correlationKey", prepared.correlationKey());
        }
        String commandKey = "M5:CONNECTOR:" + instanceId + ":" + nodeKey + ":" + execution.getId();
        JSONObject response = bpmConnectorInvocationService.invokePersistent(
                commandKey, instanceId, connector, version, operation, request);
        mapResponse(execution, response, snapshot.getJSONObject("responseMapping"));
        execution.setVariable("externalResponse_" + nodeKey, response);
    }

    private BpmDefinitionNodeEntity runtimeNodeSnapshot(BpmInstanceEntity instance, String authoredNodeId) {
        if (!"GRAPH".equals(instance.getDefinitionSource())) {
            return bpmDefinitionNodeDao.selectOne(Wrappers.<BpmDefinitionNodeEntity>lambdaQuery()
                    .eq(BpmDefinitionNodeEntity::getDefinitionId, instance.getDefinitionId())
                    .eq(BpmDefinitionNodeEntity::getNodeKey, authoredNodeId)
                    .last("LIMIT 1"));
        }
        BpmGraphRuntimeMetadataService.GraphNodeMetadata metadata = bpmGraphRuntimeMetadataService
                .requireNode(instance.getGraphDefinitionVersionId(), authoredNodeId);
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setNodeKey(metadata.authoredNodeId());
        node.setNodeType(metadata.nodeType().name());
        node.setNodeNameSnapshot(metadata.nodeName());
        node.setCompiledNodeSnapshotJson(metadata.properties().toJSONString());
        return node;
    }

    private JSONObject mapRequest(String formDataJson, JSONObject mapping) {
        JSONObject formData = JSON.parseObject(formDataJson);
        JSONObject request = new JSONObject(true);
        if (mapping != null) {
            for (Map.Entry<String, Object> entry : mapping.entrySet()) {
                request.put(entry.getKey(), formData == null ? null : formData.get(String.valueOf(entry.getValue())));
            }
        }
        return request;
    }

    private void mapResponse(DelegateExecution execution, JSONObject response, JSONObject mapping) {
        if (mapping == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
            execution.setVariable(String.valueOf(entry.getValue()), response.get(entry.getKey()));
        }
    }

    private Long requireInstanceId(DelegateExecution execution) {
        Object raw = execution.getVariable("hunyuanInstanceId");
        if (raw == null) {
            throw new IllegalArgumentException("HUNYUAN_INSTANCE_ID_MISSING：Flowable 缺少 Hunyuan 实例ID");
        }
        return Long.valueOf(String.valueOf(raw));
    }

    private String expressionValue(Expression expression, DelegateExecution execution) {
        return String.valueOf(expression.getValue(execution));
    }
}
