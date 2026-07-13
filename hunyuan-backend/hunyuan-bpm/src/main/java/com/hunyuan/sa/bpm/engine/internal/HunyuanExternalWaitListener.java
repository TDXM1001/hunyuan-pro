package com.hunyuan.sa.bpm.engine.internal;

import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitService;
import jakarta.annotation.Resource;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.common.engine.api.delegate.Expression;
import org.springframework.stereotype.Component;

/**
 * receive task 激活后回填真实 Flowable executionId。
 */
@Component("hunyuanExternalWaitListener")
public class HunyuanExternalWaitListener implements ExecutionListener {

    @Resource
    private BpmExternalWaitService bpmExternalWaitService;

    private Expression authoredNodeId;

    @Override
    public void notify(DelegateExecution execution) {
        String nodeKey = String.valueOf(authoredNodeId.getValue(execution));
        bpmExternalWaitService.bindExecution(
                execution.getProcessInstanceId(),
                nodeKey,
                execution.getId()
        );
    }
}
