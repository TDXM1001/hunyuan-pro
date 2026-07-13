package com.hunyuan.sa.bpm.approvaldata;

import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmApprovalSubjectSnapshotDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmProcessWorkingDataDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmTaskActionEvidenceDao;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmApprovalSubjectSnapshotEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmProcessWorkingDataEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmTaskActionEvidenceEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.WorkingDataMutationCommand;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.WorkingDataMutationResult;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalDataMutationService;
import com.hunyuan.sa.bpm.module.businesscontract.dao.BpmBusinessContractVersionDao;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmApprovalDataMutationServiceTest {

    @Test
    void updateShouldCreateNewWorkingVersionAndBeforeAfterEvidence() {
        Fixture fixture = fixture();

        WorkingDataMutationResult result = fixture.service.update(new WorkingDataMutationCommand(
                101L, 9001L, 1L, "{\"approvedAmount\":9800.25}",
                "财务核定金额", 50L, "财务审批人", "APPROVE", "同意", "[]"
        ));

        ArgumentCaptor<BpmProcessWorkingDataEntity> workingCaptor =
                ArgumentCaptor.forClass(BpmProcessWorkingDataEntity.class);
        ArgumentCaptor<BpmTaskActionEvidenceEntity> evidenceCaptor =
                ArgumentCaptor.forClass(BpmTaskActionEvidenceEntity.class);
        verify(fixture.workingDataDao).insert(workingCaptor.capture());
        verify(fixture.evidenceDao).insert(evidenceCaptor.capture());
        assertThat(workingCaptor.getValue()).satisfies(working -> {
            assertThat(working.getDataVersion()).isEqualTo(2L);
            assertThat(working.getPreviousDataVersion()).isEqualTo(1L);
            assertThat(working.getDataJson()).contains("9800.25").contains("approvalNote");
            assertThat(working.getChangeReason()).isEqualTo("财务核定金额");
        });
        assertThat(evidenceCaptor.getValue()).satisfies(evidence -> {
            assertThat(evidence.getActionType()).isEqualTo("APPROVE");
            assertThat(evidence.getBeforeWorkingDataVersion()).isEqualTo(1L);
            assertThat(evidence.getAfterWorkingDataVersion()).isEqualTo(2L);
            assertThat(evidence.getBeforeDataJson()).contains("12000.50");
            assertThat(evidence.getAfterDataJson()).contains("9800.25");
            assertThat(evidence.getActorEmployeeId()).isEqualTo(50L);
        });
        assertThat(result.dataVersion()).isEqualTo(2L);
    }

    @Test
    void updateShouldRejectStaleWorkingDataVersion() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service.update(new WorkingDataMutationCommand(
                101L, 9001L, 0L, "{\"approvedAmount\":9800.25}",
                "财务核定金额", 50L, "财务审批人", "APPROVE", "同意", "[]"
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WORKING_DATA_VERSION_CONFLICT");
    }

    @Test
    void updateShouldRejectFieldOutsideContractWhitelist() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service.update(new WorkingDataMutationCommand(
                101L, 9001L, 1L, "{\"applicantNote\":\"篡改申请依据\"}",
                "尝试越权修改", 50L, "财务审批人", "APPROVE", "同意", "[]"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicantNote");
    }

    @Test
    void updateWithoutPatchShouldRecordActionEvidenceWithoutCreatingDataVersion() {
        Fixture fixture = fixture();

        WorkingDataMutationResult result = fixture.service.update(new WorkingDataMutationCommand(
                101L, 9001L, 1L, "{}", "审批拒绝", 50L, "财务审批人", "REJECT", "资料不完整", "[]"
        ));

        verify(fixture.workingDataDao, Mockito.never()).insert(Mockito.any(BpmProcessWorkingDataEntity.class));
        ArgumentCaptor<BpmTaskActionEvidenceEntity> evidence =
                ArgumentCaptor.forClass(BpmTaskActionEvidenceEntity.class);
        verify(fixture.evidenceDao).insert(evidence.capture());
        assertThat(evidence.getValue().getBeforeWorkingDataVersion()).isEqualTo(1L);
        assertThat(evidence.getValue().getAfterWorkingDataVersion()).isEqualTo(1L);
        assertThat(evidence.getValue().getChangedFieldsJson()).isEqualTo("[]");
        assertThat(result.dataVersion()).isEqualTo(1L);
        assertThat(result.processWorkingDataId()).isEqualTo(301L);
    }

    @Test
    void evidenceDigestShouldCoverCommentAndAttachments() {
        Fixture first = fixture();
        first.service.update(new WorkingDataMutationCommand(
                101L, 9001L, 1L, "{}", "审批通过", 50L, "财务审批人",
                "APPROVE", "同意原方案", "[{\"fileKey\":\"decision-a.pdf\"}]"
        ));
        ArgumentCaptor<BpmTaskActionEvidenceEntity> firstEvidence =
                ArgumentCaptor.forClass(BpmTaskActionEvidenceEntity.class);
        verify(first.evidenceDao).insert(firstEvidence.capture());

        Fixture second = fixture();
        second.service.update(new WorkingDataMutationCommand(
                101L, 9001L, 1L, "{}", "审批通过", 50L, "财务审批人",
                "APPROVE", "同意调整方案", "[{\"fileKey\":\"decision-b.pdf\"}]"
        ));
        ArgumentCaptor<BpmTaskActionEvidenceEntity> secondEvidence =
                ArgumentCaptor.forClass(BpmTaskActionEvidenceEntity.class);
        verify(second.evidenceDao).insert(secondEvidence.capture());

        assertThat(firstEvidence.getValue().getEvidenceDigest())
                .isNotEqualTo(secondEvidence.getValue().getEvidenceDigest());
    }

    private Fixture fixture() {
        BpmApprovalSubjectSnapshotDao subjectDao = Mockito.mock(BpmApprovalSubjectSnapshotDao.class);
        BpmBusinessContractVersionDao contractDao = Mockito.mock(BpmBusinessContractVersionDao.class);
        BpmProcessWorkingDataDao workingDataDao = Mockito.mock(BpmProcessWorkingDataDao.class);
        BpmTaskActionEvidenceDao evidenceDao = Mockito.mock(BpmTaskActionEvidenceDao.class);
        BpmApprovalSubjectSnapshotEntity subject = new BpmApprovalSubjectSnapshotEntity();
        subject.setApprovalSubjectSnapshotId(101L);
        subject.setBusinessContractVersionId(22L);
        when(subjectDao.selectById(101L)).thenReturn(subject);
        when(contractDao.selectById(22L)).thenReturn(contract());
        BpmProcessWorkingDataEntity current = new BpmProcessWorkingDataEntity();
        current.setProcessWorkingDataId(301L);
        current.setApprovalSubjectSnapshotId(101L);
        current.setDataVersion(1L);
        current.setDataJson("{\"approvedAmount\":12000.50,\"approvalNote\":null}");
        when(workingDataDao.selectLatestBySubjectForUpdate(101L)).thenReturn(current);
        return new Fixture(
                new BpmApprovalDataMutationService(subjectDao, contractDao, workingDataDao, evidenceDao),
                workingDataDao,
                evidenceDao
        );
    }

    private BpmBusinessContractVersionEntity contract() {
        BpmBusinessContractVersionEntity entity = new BpmBusinessContractVersionEntity();
        entity.setBusinessContractVersionId(22L);
        entity.setContractJson("""
                {
                  "workingDataSchema":[
                    {"key":"approvedAmount","type":"DECIMAL","required":true},
                    {"key":"approvalNote","type":"STRING","required":false}
                  ],
                  "changePolicy":{"mode":"FIELD_CONTROLLED","editableFields":["approvedAmount","approvalNote"]}
                }
                """);
        return entity;
    }

    private record Fixture(
            BpmApprovalDataMutationService service,
            BpmProcessWorkingDataDao workingDataDao,
            BpmTaskActionEvidenceDao evidenceDao
    ) {
    }
}
