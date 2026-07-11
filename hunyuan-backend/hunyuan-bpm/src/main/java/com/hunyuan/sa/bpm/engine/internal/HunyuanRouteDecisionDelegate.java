package com.hunyuan.sa.bpm.engine.internal;

import com.hunyuan.sa.bpm.module.runtime.service.BpmRouteDecisionCommand;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRouteDecisionResult;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRouteDecisionService;
import jakarta.annotation.Resource;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Flowable 只通过该固定入口请求 Hunyuan 计算路由。
 */
@Component("hunyuanRouteDecisionDelegate")
public class HunyuanRouteDecisionDelegate implements JavaDelegate {

    @Resource
    private BpmRouteDecisionService bpmRouteDecisionService;

    private Expression routeNodeKey;

    @Override
    public void execute(DelegateExecution execution) {
        Object rawInstanceId = execution.getVariable("hunyuanInstanceId");
        if (rawInstanceId == null) {
            throw new IllegalArgumentException("HUNYUAN_INSTANCE_ID_MISSING：Flowable 缺少 Hunyuan 实例ID");
        }
        String nodeKey = String.valueOf(routeNodeKey.getValue(execution));
        BpmRouteDecisionResult result = bpmRouteDecisionService.evaluateAndRecord(
                new BpmRouteDecisionCommand(
                        Long.valueOf(String.valueOf(rawInstanceId)),
                        execution.getProcessInstanceId(),
                        nodeKey
                )
        );
        bpmRouteDecisionService.writeBranchVariables(execution, nodeKey, result.matchedBranchKeys());
    }
}
