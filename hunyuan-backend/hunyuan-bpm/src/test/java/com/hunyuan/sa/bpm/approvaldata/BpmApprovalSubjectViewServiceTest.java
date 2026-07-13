package com.hunyuan.sa.bpm.approvaldata;

import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmApprovalSubjectSnapshotDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmProcessWorkingDataDao;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmApprovalSubjectSnapshotEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.vo.BpmApprovalSubjectContextVO;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalSubjectViewService;
import com.hunyuan.sa.bpm.module.businesscontract.dao.BpmBusinessContractVersionDao;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmApprovalSubjectViewServiceTest {

    @Test
    void buildForTaskShouldIntersectSensitivityAndNodeFieldPermissions() {
        BpmApprovalSubjectSnapshotDao subjectDao = Mockito.mock(BpmApprovalSubjectSnapshotDao.class);
        BpmProcessWorkingDataDao workingDao = Mockito.mock(BpmProcessWorkingDataDao.class);
        BpmBusinessContractVersionDao contractDao = Mockito.mock(BpmBusinessContractVersionDao.class);
        GraphDefinitionVersionDao graphDao = Mockito.mock(GraphDefinitionVersionDao.class);
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        BpmApprovalSubjectViewService service = new BpmApprovalSubjectViewService(
                subjectDao, workingDao, contractDao, graphDao, stageDao
        );
        when(subjectDao.selectById(101L)).thenReturn(subject());
        when(workingDao.selectById(301L)).thenReturn(workingData());
        when(contractDao.selectById(22L)).thenReturn(contract());
        when(graphDao.selectById(41L)).thenReturn(graph());
        BpmApprovalStageEntity stage = new BpmApprovalStageEntity();
        stage.setApprovalStageId(71L);
        stage.setAuthoredNodeId("finance-review");
        when(stageDao.selectById(71L)).thenReturn(stage);
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setApprovalSubjectSnapshotId(101L);
        instance.setProcessWorkingDataId(301L);
        instance.setGraphDefinitionVersionId(41L);
        BpmTaskEntity task = new BpmTaskEntity();
        task.setApprovalStageId(71L);

        BpmApprovalSubjectContextVO context = service.buildForTask(task, instance);

        assertThat(context.getViewState()).isEqualTo("READY");
        assertThat(context.getTitle()).isEqualTo("设备采购申请");
        assertThat(context.getFieldsJson())
                .contains("amount", "applicantNote")
                .doesNotContain("internalCostCode", "RND-01");
        assertThat(context.getLineItemsJson()).contains("开发工作站");
        assertThat(context.getAttachmentsJson()).contains("报价单.pdf");
        assertThat(context.getWorkingDataJson()).contains("approvedAmount").doesNotContain("privateMemo");
        assertThat(context.getFieldPermissions())
                .extracting(item -> item.getFieldKey() + ":" + item.getPermission())
                .containsExactly("amount:READONLY", "applicantNote:READONLY", "approvedAmount:EDITABLE");
    }

    private BpmApprovalSubjectSnapshotEntity subject() {
        BpmApprovalSubjectSnapshotEntity subject = new BpmApprovalSubjectSnapshotEntity();
        subject.setApprovalSubjectSnapshotId(101L);
        subject.setBusinessContractVersionId(22L);
        subject.setTitle("设备采购申请");
        subject.setSummary("申请采购研发设备");
        subject.setFieldsJson("{\"amount\":12000.50,\"applicantNote\":\"研发使用\",\"internalCostCode\":\"RND-01\"}");
        subject.setLineItemsJson("[{\"name\":\"开发工作站\",\"quantity\":2}]");
        subject.setAttachmentsJson("[{\"fileKey\":\"quote.pdf\",\"fileName\":\"报价单.pdf\"}]");
        return subject;
    }

    private com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmProcessWorkingDataEntity workingData() {
        var working = new com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmProcessWorkingDataEntity();
        working.setProcessWorkingDataId(301L);
        working.setDataVersion(2L);
        working.setDataJson("{\"approvedAmount\":11000.00,\"privateMemo\":\"仅主管可见\"}");
        return working;
    }

    private BpmBusinessContractVersionEntity contract() {
        BpmBusinessContractVersionEntity contract = new BpmBusinessContractVersionEntity();
        contract.setBusinessContractVersionId(22L);
        contract.setContractJson("""
                {"fieldSchema":[
                  {"key":"amount","sensitivity":"INTERNAL"},
                  {"key":"applicantNote","sensitivity":"INTERNAL"},
                  {"key":"internalCostCode","sensitivity":"CONFIDENTIAL"}
                ],"workingDataSchema":[
                  {"key":"approvedAmount","sensitivity":"INTERNAL"},
                  {"key":"privateMemo","sensitivity":"CONFIDENTIAL"}
                ]}
                """);
        return contract;
    }

    private GraphDefinitionVersionEntity graph() {
        GraphDefinitionVersionEntity graph = new GraphDefinitionVersionEntity();
        graph.setGraphDefinitionVersionId(41L);
        graph.setGraphSnapshotJson("""
                {"nodes":[{"nodeId":"finance-review","properties":{"maxSensitivity":"INTERNAL","fieldPermissions":[
                  {"fieldKey":"amount","permission":"READONLY"},
                  {"fieldKey":"internalCostCode","permission":"READONLY"},
                  {"fieldKey":"approvedAmount","permission":"EDITABLE"}
                ]}}]}
                """);
        return graph;
    }
}
