package com.hunyuan.sa.bpm.engine.internal;

import com.hunyuan.sa.bpm.module.runtime.service.BpmSubProcessService;
import jakarta.annotation.Resource;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component("hunyuanSubProcessInstanceStartListener")
public class HunyuanSubProcessInstanceStartListener implements ExecutionListener {

    @Resource
    private BpmSubProcessService bpmSubProcessService;

    @Override
    public void notify(DelegateExecution execution) {
        Object rawParentId = execution.getVariable("hunyuanParentInstanceId");
        Object rawChildId = execution.getVariable("hunyuanInstanceId");
        if (rawParentId == null || rawChildId == null) {
            return;
        }
        bpmSubProcessService.bindChildEngineInstance(
                Long.valueOf(String.valueOf(rawParentId)),
                Long.valueOf(String.valueOf(rawChildId)),
                execution.getProcessInstanceId()
        );
    }
}
