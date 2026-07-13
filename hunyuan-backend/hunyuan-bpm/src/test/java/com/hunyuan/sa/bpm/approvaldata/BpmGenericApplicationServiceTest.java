package com.hunyuan.sa.bpm.approvaldata;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.ApprovalSubjectCreationResult;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.GenericApplicationSubmitCommand;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.GenericApplicationSubmitResult;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalSubjectService;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmGenericApplicationService;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmGenericApplicationServiceTest {

    @Test
    void submitShouldCreateApprovalSubjectAndStartGraphWithItsFrozenId() {
        BpmApprovalSubjectService subjectService = Mockito.mock(BpmApprovalSubjectService.class);
        BpmInstanceService instanceService = Mockito.mock(BpmInstanceService.class);
        BpmCurrentActorProvider actorProvider = Mockito.mock(BpmCurrentActorProvider.class);
        BpmOrgIdentityGateway identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        when(actorProvider.requireCurrentEmployeeId()).thenReturn(20L);
        when(identityGateway.requireEmployee(20L)).thenReturn(new BpmEmployeeSnapshot(
                20L, "申请人", 3L, "研发部", null, null
        ));
        when(subjectService.create(any())).thenReturn(new ApprovalSubjectCreationResult(
                101L, 201L, 301L, 1L, 1L, 1L
        ));
        when(instanceService.startInstance(any())).thenReturn(ResponseDTO.ok(81L));
        BpmGenericApplicationService service = new BpmGenericApplicationService(
                subjectService, instanceService, actorProvider, identityGateway
        );

        GenericApplicationSubmitResult result = service.submit(command());

        ArgumentCaptor<BpmInstanceStartForm> startForm = ArgumentCaptor.forClass(BpmInstanceStartForm.class);
        verify(instanceService).startInstance(startForm.capture());
        assertThat(startForm.getValue().getGraphDefinitionVersionId()).isEqualTo(41L);
        assertThat(startForm.getValue().getApprovalSubjectSnapshotId()).isEqualTo(101L);
        assertThat(result.approvalSubjectSnapshotId()).isEqualTo(101L);
        assertThat(result.instanceId()).isEqualTo(81L);
    }

    private GenericApplicationSubmitCommand command() {
        return new GenericApplicationSubmitCommand(
                41L, "generic-application", 1, "HUNYUAN", "GENERIC_APPLICATION", "REQ-2026-0001",
                "设备采购申请", "申请采购研发设备",
                "{\"amount\":12000.50,\"applicantNote\":\"研发使用\"}",
                "[{\"name\":\"开发工作站\",\"quantity\":2}]",
                "[{\"fileKey\":\"quote.pdf\",\"fileName\":\"报价单.pdf\"}]",
                "{\"financeApprover\":50}",
                "{\"approvedAmount\":12000.50,\"approvalNote\":null}"
        );
    }
}
