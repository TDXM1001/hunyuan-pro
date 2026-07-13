package com.hunyuan.sa.bpm.approvaldata;

import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmApprovalSubjectSnapshotDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmProcessWorkingDataDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmRoutingFactSnapshotDao;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmApprovalSubjectSnapshotEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmProcessWorkingDataEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmRoutingFactSnapshotEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.ApprovalSubjectCreateCommand;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.ApprovalSubjectCreationResult;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalSubjectService;
import com.hunyuan.sa.bpm.module.businesscontract.dao.BpmBusinessContractVersionDao;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmApprovalSubjectServiceTest {

    @Test
    void createShouldValidateContractAndSeparateFrozenDataPlanes() {
        BpmBusinessContractVersionDao contractDao = Mockito.mock(BpmBusinessContractVersionDao.class);
        BpmApprovalSubjectSnapshotDao subjectDao = Mockito.mock(BpmApprovalSubjectSnapshotDao.class);
        BpmRoutingFactSnapshotDao routingFactDao = Mockito.mock(BpmRoutingFactSnapshotDao.class);
        BpmProcessWorkingDataDao workingDataDao = Mockito.mock(BpmProcessWorkingDataDao.class);
        when(contractDao.selectActiveByKeyAndVersion("generic-application", 1)).thenReturn(activeContract());
        when(subjectDao.selectCount(any())).thenReturn(0L);
        BpmApprovalSubjectService service = new BpmApprovalSubjectService(
                contractDao, subjectDao, routingFactDao, workingDataDao
        );

        ApprovalSubjectCreationResult result = service.create(new ApprovalSubjectCreateCommand(
                "generic-application",
                1,
                "HUNYUAN",
                "GENERIC_APPLICATION",
                "REQ-2026-0001",
                "设备采购申请",
                "申请采购研发设备",
                "{\"amount\":12000.50,\"applicantNote\":\"研发使用\",\"internalCostCode\":\"RND-01\"}",
                "[{\"name\":\"开发工作站\",\"quantity\":2}]",
                "[{\"fileKey\":\"quote.pdf\",\"fileName\":\"报价单.pdf\"}]",
                "{\"financeApprover\":50,\"internalCostCode\":\"RND-01\"}",
                "{\"approvedAmount\":12000.50,\"approvalNote\":null}",
                20L,
                "申请人"
        ));

        ArgumentCaptor<BpmApprovalSubjectSnapshotEntity> subjectCaptor =
                ArgumentCaptor.forClass(BpmApprovalSubjectSnapshotEntity.class);
        ArgumentCaptor<BpmRoutingFactSnapshotEntity> routingCaptor =
                ArgumentCaptor.forClass(BpmRoutingFactSnapshotEntity.class);
        ArgumentCaptor<BpmProcessWorkingDataEntity> workingCaptor =
                ArgumentCaptor.forClass(BpmProcessWorkingDataEntity.class);
        verify(subjectDao).insert(subjectCaptor.capture());
        verify(routingFactDao).insert(routingCaptor.capture());
        verify(workingDataDao).insert(workingCaptor.capture());

        assertThat(subjectCaptor.getValue()).satisfies(subject -> {
            assertThat(subject.getSourceSystem()).isEqualTo("HUNYUAN");
            assertThat(subject.getBusinessType()).isEqualTo("GENERIC_APPLICATION");
            assertThat(subject.getBusinessKey()).isEqualTo("REQ-2026-0001");
            assertThat(subject.getFieldsJson()).contains("internalCostCode");
            assertThat(subject.getSubjectVersion()).isEqualTo(1L);
        });
        assertThat(routingCaptor.getValue()).satisfies(routing -> {
            assertThat(routing.getFactsJson()).contains("financeApprover");
            assertThat(routing.getFactsJson()).doesNotContain("internalCostCode");
            assertThat(routing.getAllowedFactKeysJson()).contains("financeApprover");
            assertThat(routing.getRoutingFactVersion()).isEqualTo(1L);
        });
        assertThat(workingCaptor.getValue()).satisfies(working -> {
            assertThat(working.getDataJson()).contains("approvedAmount");
            assertThat(working.getDataVersion()).isEqualTo(1L);
        });
        assertThat(result.subjectVersion()).isEqualTo(1L);
        assertThat(result.routingFactVersion()).isEqualTo(1L);
        assertThat(result.workingDataVersion()).isEqualTo(1L);
    }

    @Test
    void createShouldRejectDuplicateBusinessObjectIdentity() {
        BpmBusinessContractVersionDao contractDao = Mockito.mock(BpmBusinessContractVersionDao.class);
        BpmApprovalSubjectSnapshotDao subjectDao = Mockito.mock(BpmApprovalSubjectSnapshotDao.class);
        when(contractDao.selectActiveByKeyAndVersion("generic-application", 1)).thenReturn(activeContract());
        when(subjectDao.selectCount(any())).thenReturn(1L);
        BpmApprovalSubjectService service = new BpmApprovalSubjectService(
                contractDao,
                subjectDao,
                Mockito.mock(BpmRoutingFactSnapshotDao.class),
                Mockito.mock(BpmProcessWorkingDataDao.class)
        );

        assertThatThrownBy(() -> service.create(validCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("业务对象已存在");
    }

    @Test
    void createShouldRejectMissingRequiredRoutingFact() {
        BpmBusinessContractVersionDao contractDao = Mockito.mock(BpmBusinessContractVersionDao.class);
        when(contractDao.selectActiveByKeyAndVersion("generic-application", 1)).thenReturn(activeContract());
        BpmApprovalSubjectService service = new BpmApprovalSubjectService(
                contractDao,
                Mockito.mock(BpmApprovalSubjectSnapshotDao.class),
                Mockito.mock(BpmRoutingFactSnapshotDao.class),
                Mockito.mock(BpmProcessWorkingDataDao.class)
        );
        ApprovalSubjectCreateCommand command = new ApprovalSubjectCreateCommand(
                "generic-application", 1, "HUNYUAN", "GENERIC_APPLICATION", "REQ-2026-0002",
                "设备采购申请", "申请采购研发设备", "{\"amount\":12000.50}", "[]", "[]",
                "{}", "{\"approvedAmount\":12000.50}", 20L, "申请人"
        );

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("financeApprover");
    }

    @Test
    void createShouldRejectFieldsOutsideFrozenContractSchemas() {
        BpmBusinessContractVersionDao contractDao = Mockito.mock(BpmBusinessContractVersionDao.class);
        BpmApprovalSubjectSnapshotDao subjectDao = Mockito.mock(BpmApprovalSubjectSnapshotDao.class);
        when(contractDao.selectActiveByKeyAndVersion("generic-application", 1)).thenReturn(activeContract());
        when(subjectDao.selectCount(any())).thenReturn(0L);
        BpmApprovalSubjectService service = new BpmApprovalSubjectService(
                contractDao,
                subjectDao,
                Mockito.mock(BpmRoutingFactSnapshotDao.class),
                Mockito.mock(BpmProcessWorkingDataDao.class)
        );
        ApprovalSubjectCreateCommand command = new ApprovalSubjectCreateCommand(
                "generic-application", 1, "HUNYUAN", "GENERIC_APPLICATION", "REQ-2026-0002",
                "设备采购申请", "申请采购研发设备",
                "{\"amount\":12000.50,\"undeclaredSecret\":\"must-not-enter-snapshot\"}",
                "[]", "[]", "{\"financeApprover\":50}",
                "{\"approvedAmount\":12000.50,\"undeclaredRuntimeValue\":true}",
                20L, "申请人"
        );

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未在业务契约中声明");
    }

    private ApprovalSubjectCreateCommand validCommand() {
        return new ApprovalSubjectCreateCommand(
                "generic-application", 1, "HUNYUAN", "GENERIC_APPLICATION", "REQ-2026-0001",
                "设备采购申请", "申请采购研发设备", "{\"amount\":12000.50}", "[]", "[]",
                "{\"financeApprover\":50}", "{\"approvedAmount\":12000.50}", 20L, "申请人"
        );
    }

    private BpmBusinessContractVersionEntity activeContract() {
        BpmBusinessContractVersionEntity entity = new BpmBusinessContractVersionEntity();
        entity.setBusinessContractVersionId(22L);
        entity.setContractKey("generic-application");
        entity.setContractVersion(1);
        entity.setLifecycleState("ACTIVE");
        entity.setContractJson("""
                {
                  "sourceSystem":"HUNYUAN",
                  "businessType":"GENERIC_APPLICATION",
                  "businessKeyRule":{"pattern":"REQ-[0-9]{4}-[0-9]{4}"},
                  "fieldSchema":[
                    {"key":"amount","type":"DECIMAL","required":true,"sensitivity":"INTERNAL"},
                    {"key":"applicantNote","type":"STRING","required":false,"sensitivity":"INTERNAL"},
                    {"key":"internalCostCode","type":"STRING","required":false,"sensitivity":"CONFIDENTIAL"}
                  ],
                  "routingFacts":[
                    {"key":"financeApprover","type":"EMPLOYEE_ID","required":true,"sensitivity":"INTERNAL","candidateUsable":true}
                  ],
                  "workingDataSchema":[
                    {"key":"approvedAmount","type":"DECIMAL","required":true},
                    {"key":"approvalNote","type":"STRING","required":false}
                  ],
                  "attachmentRules":{"maxCount":5},
                  "detailLayout":{"sections":["fields","lineItems","attachments"]},
                  "changePolicy":{"mode":"FIELD_CONTROLLED","editableFields":["approvedAmount","approvalNote"]}
                }
                """);
        return entity;
    }
}
