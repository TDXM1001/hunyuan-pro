package com.hunyuan.sa.bpm.engine.internal;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Flowable 流程实例运行网关，仅在 BPM 模块内部使用。
 */
@Component
public class FlowableProcessInstanceGateway {

    private static final String APPROVAL_COMPLETION_MARKER_PREFIX = "hunyuanApprovalStageCompleted_";

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private HistoryService historyService;

    /**
     * 启动流程实例，并返回 Flowable 内部实例 ID。
     */
    public String start(
            String engineProcessDefinitionId,
            Long employeeId,
            String formDataJson,
            Map<String, Object> runtimeAssignmentVariables
    ) {
        return start(engineProcessDefinitionId, null, employeeId, formDataJson, runtimeAssignmentVariables);
    }

    /**
     * 启动需要执行 Hunyuan delegate 的流程，并显式传入平台实例 ID。
     */
    public String start(
            String engineProcessDefinitionId,
            Long hunyuanInstanceId,
            Long employeeId,
            String formDataJson,
            Map<String, Object> runtimeAssignmentVariables
    ) {
        Map<String, Object> variables = new HashMap<>();
        if (hunyuanInstanceId != null) {
            variables.put("hunyuanInstanceId", hunyuanInstanceId);
        }
        variables.put("startEmployeeId", employeeId);
        variables.put("formDataJson", formDataJson);

        JSONObject formDataObject = JSON.parseObject(formDataJson);
        if (formDataObject != null) {
            variables.put("formData", formDataObject);
        }
        if (runtimeAssignmentVariables != null && !runtimeAssignmentVariables.isEmpty()) {
            variables.putAll(runtimeAssignmentVariables);
        }

        ProcessInstance processInstance = runtimeService.startProcessInstanceById(engineProcessDefinitionId, variables);
        return processInstance.getProcessInstanceId();
    }

    /**
     * 取消指定流程实例。
     */
    public void cancel(String engineProcessInstanceId, String reason) {
        runtimeService.deleteProcessInstance(engineProcessInstanceId, reason);
    }

    /**
     * 恢复受控等待执行，并只写入单个登记变量。
     */
    public void trigger(String executionId, String variableName, Object variableValue) {
        runtimeService.trigger(executionId, Map.of(variableName, variableValue));
    }

    /**
     * 为每次审批阶段调用写入独立标记，供崩溃后的历史对账使用。
     */
    public static String approvalStageCompletionMarker(String stageInvocationId) {
        if (stageInvocationId == null || stageInvocationId.isBlank()) {
            throw new IllegalArgumentException("审批阶段调用标识不能为空");
        }
        return APPROVAL_COMPLETION_MARKER_PREFIX + stageInvocationId;
    }

    /**
     * 只读取 Flowable 已持久化的历史证据，绝不在对账过程中重放引擎副作用。
     */
    public EngineEffectObservation inspectApprovalStageEffect(
            String engineProcessInstanceId,
            String stageInvocationId,
            String terminalReason
    ) {
        if (isBlank(engineProcessInstanceId) || isBlank(stageInvocationId) || isBlank(terminalReason)) {
            throw new IllegalArgumentException("审批阶段引擎对账参数不完整");
        }
        if ("APPROVED".equals(terminalReason)) {
            HistoricVariableInstance marker = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(engineProcessInstanceId)
                    .variableName(approvalStageCompletionMarker(stageInvocationId))
                    .singleResult();
            if (marker != null && Boolean.TRUE.equals(marker.getValue())) {
                return EngineEffectObservation.confirmed("审批完成标记已写入 Flowable 历史");
            }
            return EngineEffectObservation.unconfirmed("未找到本阶段的审批完成历史标记");
        }

        HistoricProcessInstance process = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(engineProcessInstanceId)
                .finished()
                .singleResult();
        if (process != null && terminalReason.equals(process.getDeleteReason())) {
            return EngineEffectObservation.confirmed("流程取消原因已写入 Flowable 历史");
        }
        return EngineEffectObservation.unconfirmed("未找到匹配的流程取消历史原因");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record EngineEffectObservation(boolean confirmed, String reason) {

        public static EngineEffectObservation confirmed(String reason) {
            return new EngineEffectObservation(true, reason);
        }

        public static EngineEffectObservation unconfirmed(String reason) {
            return new EngineEffectObservation(false, reason);
        }
    }
}
