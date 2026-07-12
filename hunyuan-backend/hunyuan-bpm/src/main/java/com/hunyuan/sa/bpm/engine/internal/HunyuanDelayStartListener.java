package com.hunyuan.sa.bpm.engine.internal;

import com.hunyuan.sa.bpm.module.runtime.service.BpmTimeEventService;
import jakarta.annotation.Resource;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * 延迟节点激活投影入口。
 */
@Component("hunyuanDelayStartListener")
public class HunyuanDelayStartListener implements ExecutionListener {
    @Resource
    private BpmTimeEventService bpmTimeEventService;

    @Override
    public void notify(DelegateExecution execution) {
        Object rawInstanceId = execution.getVariable("hunyuanInstanceId");
        if (rawInstanceId == null) {
            throw new IllegalArgumentException("HUNYUAN_INSTANCE_ID_MISSING：Flowable 缺少 Hunyuan 实例ID");
        }
        bpmTimeEventService.scheduleDelay(
                Long.valueOf(String.valueOf(rawInstanceId)),
                execution.getProcessInstanceId(),
                execution.getId(),
                execution.getCurrentActivityId()
        );
    }
}
