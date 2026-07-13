package com.hunyuan.sa.bpm.engine.internal;

import com.hunyuan.sa.bpm.module.runtime.service.BpmSubProcessService;
import jakarta.annotation.Resource;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component("hunyuanSubProcessLifecycleListener")
public class HunyuanSubProcessLifecycleListener implements ExecutionListener {
    @Resource private BpmSubProcessService bpmSubProcessService;
    private Expression authoredNodeId;

    @Override
    public void notify(DelegateExecution execution) {
        String nodeId = String.valueOf(authoredNodeId.getValue(execution));
        if (EVENTNAME_START.equals(execution.getEventName())) {
            Object rawParentId = execution.getVariable("hunyuanInstanceId");
            if (rawParentId == null) throw new IllegalArgumentException("HUNYUAN_PARENT_INSTANCE_ID_MISSING");
            Long parentInstanceId = Long.valueOf(String.valueOf(rawParentId));
            var prepared = bpmSubProcessService.prepareChild(
                    parentInstanceId, execution.getId(), nodeId, new HashMap<>(execution.getVariables()));
            execution.setVariableLocal("hunyuanParentInstanceId", parentInstanceId);
            execution.setVariableLocal("hunyuanChildInstanceId", prepared.childInstanceId());
        } else if (EVENTNAME_END.equals(execution.getEventName())) {
            Object rawParentId = execution.getVariable("hunyuanParentInstanceId");
            if (rawParentId == null) throw new IllegalArgumentException("HUNYUAN_PARENT_INSTANCE_ID_MISSING");
            Long parentInstanceId = Long.valueOf(String.valueOf(rawParentId));
            bpmSubProcessService.complete(parentInstanceId, nodeId, new HashMap<>(execution.getVariables()));
        }
    }
}
