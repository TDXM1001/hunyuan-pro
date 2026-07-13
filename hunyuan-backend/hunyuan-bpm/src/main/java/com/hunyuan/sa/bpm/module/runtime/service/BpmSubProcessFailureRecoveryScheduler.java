package com.hunyuan.sa.bpm.module.runtime.service;

import jakarta.annotation.Resource;
import org.flowable.engine.ManagementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 将 Flowable 最终失败 job 投影为可解释的子流程失败事实。 */
@Component
public class BpmSubProcessFailureRecoveryScheduler {

    @Resource
    private ManagementService managementService;

    @Resource
    private BpmSubProcessService bpmSubProcessService;

    @Scheduled(fixedDelayString = "${hunyuan.bpm.m5.sub-process-failure-scan-delay-ms:60000}")
    public void scan() {
        managementService.createDeadLetterJobQuery().listPage(0, 100).forEach(job -> {
            if (job.getProcessInstanceId() != null) {
                bpmSubProcessService.recordTechnicalFailure(
                        job.getProcessInstanceId(),
                        job.getExceptionMessage() == null ? "子流程引擎执行失败" : job.getExceptionMessage());
            }
        });
    }
}
