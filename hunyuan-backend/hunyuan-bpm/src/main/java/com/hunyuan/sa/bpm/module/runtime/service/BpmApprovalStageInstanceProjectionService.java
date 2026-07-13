package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在 Flowable 历史确认流程结束后，幂等收敛 Hunyuan 实例投影。
 */
@Service
public class BpmApprovalStageInstanceProjectionService {

    private final BpmInstanceDao bpmInstanceDao;
    private final FlowableProcessInstanceGateway flowableProcessInstanceGateway;

    public BpmApprovalStageInstanceProjectionService(
            BpmInstanceDao bpmInstanceDao,
            FlowableProcessInstanceGateway flowableProcessInstanceGateway
    ) {
        this.bpmInstanceDao = bpmInstanceDao;
        this.flowableProcessInstanceGateway = flowableProcessInstanceGateway;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean reconcileApprovedCompletion(Long instanceId, String engineProcessInstanceId) {
        if (instanceId == null || instanceId <= 0
                || engineProcessInstanceId == null || engineProcessInstanceId.isBlank()) {
            throw new IllegalArgumentException("审批通过实例投影参数不完整");
        }
        if (!flowableProcessInstanceGateway.isProcessFinished(engineProcessInstanceId)) {
            return false;
        }
        if (bpmInstanceDao.finishApprovedIfRunning(instanceId) == 1) {
            return true;
        }
        BpmInstanceEntity instance = bpmInstanceDao.selectById(instanceId);
        return instance != null
                && BpmInstanceRunStateEnum.FINISHED.equalsValue(instance.getRunState())
                && BpmInstanceResultStateEnum.APPROVED.equalsValue(instance.getResultState());
    }
}
