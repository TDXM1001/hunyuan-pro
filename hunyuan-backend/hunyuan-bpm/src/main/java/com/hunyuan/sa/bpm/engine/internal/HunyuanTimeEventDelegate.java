package com.hunyuan.sa.bpm.engine.internal;

import com.hunyuan.sa.bpm.module.runtime.service.BpmTimeEventService;
import jakarta.annotation.Resource;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Flowable 时间信号进入 Hunyuan 命令边界的固定入口。
 */
@Component("hunyuanTimeEventDelegate")
public class HunyuanTimeEventDelegate implements JavaDelegate {

    @Resource
    private BpmTimeEventService bpmTimeEventService;

    private Expression timeEventKind;

    private Expression authoredNodeKey;

    @Override
    public void execute(DelegateExecution execution) {
        String eventKind = String.valueOf(timeEventKind.getValue(execution));
        String nodeKey = String.valueOf(authoredNodeKey.getValue(execution));
        bpmTimeEventService.trigger(
                execution.getProcessInstanceId(),
                nodeKey,
                eventKind,
                execution.getId(),
                null
        );
    }
}
