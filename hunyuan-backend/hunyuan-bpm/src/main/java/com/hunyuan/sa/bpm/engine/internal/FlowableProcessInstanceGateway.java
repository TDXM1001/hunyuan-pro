package com.hunyuan.sa.bpm.engine.internal;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Flowable 流程实例运行网关，仅在 BPM 模块内部使用。
 */
@Component
public class FlowableProcessInstanceGateway {

    @Resource
    private RuntimeService runtimeService;

    /**
     * 启动流程实例，并返回 Flowable 内部实例 ID。
     */
    public String start(
            String engineProcessDefinitionId,
            Long employeeId,
            String formDataJson,
            Map<String, Object> runtimeAssignmentVariables
    ) {
        Map<String, Object> variables = new HashMap<>();
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
}
