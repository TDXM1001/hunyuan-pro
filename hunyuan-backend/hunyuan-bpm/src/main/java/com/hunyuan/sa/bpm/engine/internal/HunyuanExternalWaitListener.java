package com.hunyuan.sa.bpm.engine.internal;

import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitService;
import jakarta.annotation.Resource;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * receive task 激活后回填真实 Flowable executionId。
 */
@Component("hunyuanExternalWaitListener")
public class HunyuanExternalWaitListener implements ExecutionListener {

    private static final String WAIT_SUFFIX = "_wait";

    @Resource
    private BpmExternalWaitService bpmExternalWaitService;

    @Override
    public void notify(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        if (activityId == null || !activityId.endsWith(WAIT_SUFFIX)) {
            throw new IllegalStateException("外部等待监听器只能绑定 receive task");
        }
        String nodeKey = activityId.substring(0, activityId.length() - WAIT_SUFFIX.length());
        bpmExternalWaitService.bindExecution(
                execution.getProcessInstanceId(),
                nodeKey,
                execution.getId()
        );
    }
}
