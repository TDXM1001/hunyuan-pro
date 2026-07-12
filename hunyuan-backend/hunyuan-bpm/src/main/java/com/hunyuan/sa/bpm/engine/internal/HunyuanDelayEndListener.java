package com.hunyuan.sa.bpm.engine.internal;

import com.hunyuan.sa.bpm.module.runtime.service.BpmTimeEventService;
import jakarta.annotation.Resource;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * 延迟节点到期完成投影入口。
 */
@Component("hunyuanDelayEndListener")
public class HunyuanDelayEndListener implements ExecutionListener {
    @Resource
    private BpmTimeEventService bpmTimeEventService;

    @Override
    public void notify(DelegateExecution execution) {
        bpmTimeEventService.completeDelay(execution.getId());
    }
}
