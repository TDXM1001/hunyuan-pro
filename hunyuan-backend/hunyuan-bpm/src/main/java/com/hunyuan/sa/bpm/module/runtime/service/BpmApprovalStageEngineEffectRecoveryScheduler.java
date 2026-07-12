package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 扫描事务提交后可能遗留的审批阶段引擎副作用，并交由受控恢复服务逐条处理。
 */
@Component
public class BpmApprovalStageEngineEffectRecoveryScheduler {

    private static final int BATCH_SIZE = 50;

    @Resource
    private BpmApprovalStageDao bpmApprovalStageDao;

    @Resource
    private BpmApprovalStageEngineEffectRecoveryService recoveryService;

    @Scheduled(fixedDelay = 60_000L)
    public void scanRecoverableStageEffects() {
        bpmApprovalStageDao.selectRecoverableStageInvocationIds(BATCH_SIZE)
                .forEach(recoveryService::recover);
    }
}
