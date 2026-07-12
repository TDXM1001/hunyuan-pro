package com.hunyuan.sa.bpm.engine.internal;

import com.hunyuan.sa.bpm.engine.graph.ApprovalStageControl;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageActivationService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageService;
import jakarta.annotation.Resource;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Graph 审批 receive task 的 Flowable 监听器和唯一推进端口。
 */
@Component("hunyuanApprovalStageControl")
public class HunyuanApprovalStageControl implements ExecutionListener, ApprovalStageControl {

    @Resource
    private GraphDefinitionVersionDao graphDefinitionVersionDao;

    @Resource
    private BpmApprovalStageActivationService bpmApprovalStageActivationService;

    @Resource
    private BpmApprovalStageService bpmApprovalStageService;

    @Resource
    private FlowableProcessInstanceGateway flowableProcessInstanceGateway;

    @Override
    public void notify(DelegateExecution execution) {
        Long instanceId = requireInstanceId(execution);
        String engineProcessDefinitionId = execution.getProcessDefinitionId();
        if (isBlank(engineProcessDefinitionId)) {
            throw new IllegalStateException("Graph 审批等待点缺少 Flowable 流程定义ID");
        }
        GraphDefinitionVersionEntity version = graphDefinitionVersionDao
                .selectByEngineProcessDefinitionId(engineProcessDefinitionId);
        if (version == null || version.getGraphDefinitionVersionId() == null) {
            throw new IllegalStateException("Graph 审批等待点未绑定已发布定义版本");
        }
        bpmApprovalStageActivationService.activate(
                new BpmApprovalStageActivationService.ActivateApprovalStageCommand(
                        instanceId,
                        version.getGraphDefinitionVersionId(),
                        requiredText(execution.getCurrentActivityId(), "Graph 审批等待点缺少 compiled activity ID"),
                        requiredText(execution.getProcessInstanceId(), "Graph 审批等待点缺少 Flowable 流程实例ID"),
                        requiredText(execution.getId(), "Graph 审批等待点缺少 Flowable execution ID")
                )
        );
    }

    @Override
    public boolean completeOnce(String stageInvocationId) {
        BpmApprovalStageService.EngineEffectClaim claim = bpmApprovalStageService
                .claimEngineEffect(stageInvocationId, "APPROVED");
        if (claim == null) {
            return false;
        }
        try {
            flowableProcessInstanceGateway.trigger(
                    claim.engineExecutionId(),
                    FlowableProcessInstanceGateway.approvalStageCompletionMarker(claim.stageInvocationId()),
                    true
            );
        } catch (RuntimeException ex) {
            bpmApprovalStageService.markEngineEffectFailed(claim.approvalStageId(), ex.getMessage());
            throw ex;
        }
        bpmApprovalStageService.markEngineEffectCompleted(claim.approvalStageId());
        return true;
    }

    @Override
    public boolean closeOnce(String stageInvocationId, String reason) {
        BpmApprovalStageService.EngineEffectClaim claim = bpmApprovalStageService
                .claimEngineEffect(stageInvocationId, reason);
        if (claim == null) {
            return false;
        }
        try {
            flowableProcessInstanceGateway.cancel(
                    claim.engineProcessInstanceId(),
                    claim.terminalReason()
            );
        } catch (RuntimeException ex) {
            bpmApprovalStageService.markEngineEffectFailed(claim.approvalStageId(), ex.getMessage());
            throw ex;
        }
        bpmApprovalStageService.markEngineEffectCompleted(claim.approvalStageId());
        return true;
    }

    private Long requireInstanceId(DelegateExecution execution) {
        Object rawInstanceId = execution.getVariable("hunyuanInstanceId");
        if (rawInstanceId == null) {
            throw new IllegalArgumentException("HUNYUAN_INSTANCE_ID_MISSING：Flowable 缺少 Hunyuan 实例ID");
        }
        try {
            Long instanceId = Long.valueOf(String.valueOf(rawInstanceId));
            if (instanceId <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return instanceId;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("HUNYUAN_INSTANCE_ID_INVALID：Flowable 实例ID非法", ex);
        }
    }

    private String requiredText(String value, String errorMessage) {
        if (isBlank(value)) {
            throw new IllegalStateException(errorMessage);
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
