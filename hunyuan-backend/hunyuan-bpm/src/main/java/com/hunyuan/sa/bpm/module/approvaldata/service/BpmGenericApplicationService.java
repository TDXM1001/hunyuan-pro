package com.hunyuan.sa.bpm.module.approvaldata.service;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.ApprovalSubjectCreateCommand;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.ApprovalSubjectCreationResult;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.GenericApplicationSubmitCommand;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.GenericApplicationSubmitResult;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BpmGenericApplicationService {

    private final BpmApprovalSubjectService subjectService;
    private final BpmInstanceService instanceService;
    private final BpmCurrentActorProvider actorProvider;
    private final BpmOrgIdentityGateway identityGateway;

    public BpmGenericApplicationService(
            BpmApprovalSubjectService subjectService,
            BpmInstanceService instanceService,
            BpmCurrentActorProvider actorProvider,
            BpmOrgIdentityGateway identityGateway
    ) {
        this.subjectService = subjectService;
        this.instanceService = instanceService;
        this.actorProvider = actorProvider;
        this.identityGateway = identityGateway;
    }

    @Transactional(rollbackFor = Exception.class)
    public GenericApplicationSubmitResult submit(GenericApplicationSubmitCommand command) {
        if (command == null || command.graphDefinitionVersionId() == null) {
            throw new IllegalArgumentException("通用申请必须选择 Graph 定义版本");
        }
        Long employeeId = actorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot actor = identityGateway.requireEmployee(employeeId);
        ApprovalSubjectCreationResult subject = subjectService.create(new ApprovalSubjectCreateCommand(
                command.contractKey(), command.contractVersion(), command.sourceSystem(), command.businessType(),
                command.businessKey(), command.title(), command.summary(), command.fieldsJson(),
                command.lineItemsJson(), command.attachmentsJson(), command.routingFactsJson(),
                command.workingDataJson(), actor.employeeId(), actor.actualName()
        ));
        BpmInstanceStartForm startForm = new BpmInstanceStartForm();
        startForm.setGraphDefinitionVersionId(command.graphDefinitionVersionId());
        startForm.setApprovalSubjectSnapshotId(subject.approvalSubjectSnapshotId());
        startForm.setFormDataJson("{}");
        ResponseDTO<Long> startResponse = instanceService.startInstance(startForm);
        if (!Boolean.TRUE.equals(startResponse.getOk()) || startResponse.getData() == null) {
            throw new IllegalStateException("通用申请发起失败：" + startResponse.getMsg());
        }
        return new GenericApplicationSubmitResult(
                subject.approvalSubjectSnapshotId(), startResponse.getData()
        );
    }
}
