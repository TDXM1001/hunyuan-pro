package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCallbackRecordVO;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCommandRecordVO;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessIntegrationRecordService;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceTraceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmApprovalGroupDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmApprovalGroupMemberVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmNotificationRecordVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskActionLogVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceTraceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmNotificationRecordService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRuntimeGraphService;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTimeEventDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmExternalWaitDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageMemberDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmSubProcessLinkDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmSubProcessLinkEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmRuntimeGraphVO;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmFormDataChangeDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmFormDataChangeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmInstanceTraceServiceTest {

    private BpmInstanceTraceService traceService;

    private BpmInstanceService bpmInstanceService;

    private BpmBusinessIntegrationRecordService integrationRecordService;

    private BpmNotificationRecordService notificationRecordService;

    private BpmFormDataChangeDao formDataChangeDao;

    private BpmRuntimeGraphService runtimeGraphService;

    private BpmApprovalStageDao approvalStageDao;

    private BpmApprovalStageMemberDao approvalStageMemberDao;

    private BpmTaskDao bpmTaskDao;

    @BeforeEach
    void setUp() {
        traceService = new BpmInstanceTraceService();
        bpmInstanceService = Mockito.mock(BpmInstanceService.class);
        integrationRecordService = Mockito.mock(BpmBusinessIntegrationRecordService.class);
        notificationRecordService = Mockito.mock(BpmNotificationRecordService.class);
        formDataChangeDao = Mockito.mock(BpmFormDataChangeDao.class);
        runtimeGraphService = Mockito.mock(BpmRuntimeGraphService.class);
        setField(traceService, "bpmInstanceService", bpmInstanceService);
        setField(traceService, "integrationRecordService", integrationRecordService);
        setField(traceService, "notificationRecordService", notificationRecordService);
        setField(traceService, "bpmFormDataChangeDao", formDataChangeDao);
        setField(traceService, "bpmRuntimeGraphService", runtimeGraphService);
        setField(traceService, "bpmTimeEventDao", Mockito.mock(BpmTimeEventDao.class));
        setField(traceService, "bpmExternalWaitDao", Mockito.mock(BpmExternalWaitDao.class));
        setField(traceService, "bpmSubProcessLinkDao", Mockito.mock(BpmSubProcessLinkDao.class));
        approvalStageDao = Mockito.mock(BpmApprovalStageDao.class);
        approvalStageMemberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        bpmTaskDao = Mockito.mock(BpmTaskDao.class);
        setField(traceService, "bpmApprovalStageDao", approvalStageDao);
        setField(traceService, "bpmApprovalStageMemberDao", approvalStageMemberDao);
        setField(traceService, "bpmTaskDao", bpmTaskDao);
    }

    @Test
    void getTraceShouldAggregateInstanceTasksActionsCallbacksAndCommands() {
        BpmTaskVO task = new BpmTaskVO();
        task.setTaskId(11L);
        task.setTaskName("Manager approval");
        BpmTaskActionLogVO actionLog = new BpmTaskActionLogVO();
        actionLog.setActionLogId(21L);
        actionLog.setActionType("APPROVED");
        BpmInstanceDetailVO detail = new BpmInstanceDetailVO();
        detail.setInstanceId(88L);
        detail.setInstanceNo("DK20260708NO00001");
        detail.setTitle("Expense approval");
        detail.setCurrentTasks(List.of(task));
        detail.setActionLogs(List.of(actionLog));
        BpmApprovalGroupDetailVO groupDetail = new BpmApprovalGroupDetailVO();
        groupDetail.setApprovalGroupId(61L);
        groupDetail.setApprovalGroupKey("finance_review");
        groupDetail.setApprovalGroupName("财务复核");
        groupDetail.setApprovalMode("sequential");
        groupDetail.setProcessedMemberCount(1);
        groupDetail.setTotalMemberCount(3);
        BpmApprovalGroupMemberVO firstMember = new BpmApprovalGroupMemberVO();
        firstMember.setMemberIndex(1);
        BpmApprovalGroupMemberVO secondMember = new BpmApprovalGroupMemberVO();
        secondMember.setMemberIndex(2);
        groupDetail.setMembers(List.of(firstMember, secondMember));
        detail.setApprovalGroups(List.of(groupDetail));
        BpmCallbackRecordVO callbackRecord = new BpmCallbackRecordVO();
        callbackRecord.setCallbackRecordId(31L);
        callbackRecord.setInstanceId(88L);
        BpmCommandRecordVO commandRecord = new BpmCommandRecordVO();
        commandRecord.setCommandRecordId(41L);
        commandRecord.setInstanceId(88L);
        BpmNotificationRecordVO notificationRecord = new BpmNotificationRecordVO();
        notificationRecord.setNotificationRecordId(51L);
        notificationRecord.setInstanceId(88L);
        when(bpmInstanceService.getDetail(88L)).thenReturn(ResponseDTO.ok(detail));
        when(integrationRecordService.queryCallbackRecordsByInstanceId(88L)).thenReturn(List.of(callbackRecord));
        when(integrationRecordService.queryCommandRecordsByInstanceId(88L)).thenReturn(List.of(commandRecord));
        when(notificationRecordService.queryByInstanceId(88L)).thenReturn(List.of(notificationRecord));
        BpmFormDataChangeEntity formChange = new BpmFormDataChangeEntity();
        formChange.setChangeId(71L);
        formChange.setInstanceId(88L);
        formChange.setChangeSource("TASK_APPROVED");
        formChange.setActorNameSnapshot("财务张三");
        formChange.setBeforeVersion(1L);
        formChange.setAfterVersion(2L);
        formChange.setChangedFieldsJson("[\"approvedAmount\"]");
        formChange.setBeforeValuesJson("{\"approvedAmount\":100}");
        formChange.setAfterValuesJson("{\"approvedAmount\":98}");
        when(formDataChangeDao.queryByInstanceId(88L)).thenReturn(List.of(formChange));
        BpmRuntimeGraphVO graph = new BpmRuntimeGraphVO();
        graph.setNodes(List.of());
        graph.setRouteDecisions(List.of());
        when(runtimeGraphService.build(88L, false)).thenReturn(graph);
        BpmSubProcessLinkEntity subProcess = new BpmSubProcessLinkEntity();
        subProcess.setParentInstanceId(88L);
        subProcess.setParentNodeId("archive");
        subProcess.setLinkStatus("COMPLETED");
        BpmSubProcessLinkDao subProcessDao = (BpmSubProcessLinkDao) getField(traceService, "bpmSubProcessLinkDao");
        when(subProcessDao.selectList(Mockito.any())).thenReturn(List.of(subProcess));

        ResponseDTO<BpmInstanceTraceVO> response = traceService.getTrace(88L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getInstance().getInstanceId()).isEqualTo(88L);
        assertThat(response.getData().getCurrentTasks()).hasSize(1);
        assertThat(response.getData().getActionLogs()).hasSize(1);
        assertThat(response.getData().getCallbackRecords()).hasSize(1);
        assertThat(response.getData().getCommandRecords()).hasSize(1);
        assertThat(response.getData().getNotificationRecords()).hasSize(1);
        assertThat(response.getData().getApprovalGroups()).singleElement().satisfies(group -> {
            assertThat(group.getApprovalMode()).isEqualTo("sequential");
            assertThat(group.getApprovalGroupKey()).isEqualTo("finance_review");
            assertThat(group.getProcessedMemberCount()).isEqualTo(1);
            assertThat(group.getTotalMemberCount()).isEqualTo(3);
            assertThat(group.getMembers()).extracting(BpmApprovalGroupMemberVO::getMemberIndex)
                    .containsExactly(1, 2);
        });
        assertThat(response.getData().getFormDataChanges()).singleElement().satisfies(change -> {
            assertThat(change.getChangeSource()).isEqualTo("TASK_APPROVED");
            assertThat(change.getAfterVersion()).isEqualTo(2L);
            assertThat(change.getChangedFieldsJson()).contains("approvedAmount");
        });
        assertThat(response.getData().getProcessGraph()).isSameAs(graph);
        assertThat(response.getData().getSubProcesses()).singleElement()
                .satisfies(link -> assertThat(link.getParentNodeId()).isEqualTo("archive"));
    }

    @Test
    void getTraceShouldReturnDataNotExistWhenInstanceIsMissing() {
        when(bpmInstanceService.getDetail(404L)).thenReturn(ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST));

        ResponseDTO<BpmInstanceTraceVO> response = traceService.getTrace(404L);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getCode()).isEqualTo(UserErrorCode.DATA_NOT_EXIST.getCode());
    }

    @Test
    void getTraceShouldExposeFrozenApprovalStageFactsWithoutEngineIdentifiers() {
        BpmInstanceDetailVO detail = new BpmInstanceDetailVO();
        detail.setInstanceId(89L);
        detail.setCurrentTasks(List.of());
        detail.setActionLogs(List.of());
        detail.setApprovalGroups(List.of());
        when(bpmInstanceService.getDetail(89L)).thenReturn(ResponseDTO.ok(detail));
        when(integrationRecordService.queryCallbackRecordsByInstanceId(89L)).thenReturn(List.of());
        when(integrationRecordService.queryCommandRecordsByInstanceId(89L)).thenReturn(List.of());
        when(notificationRecordService.queryByInstanceId(89L)).thenReturn(List.of());
        when(formDataChangeDao.queryByInstanceId(89L)).thenReturn(List.of());
        BpmRuntimeGraphVO graph = new BpmRuntimeGraphVO();
        graph.setRouteDecisions(List.of());
        when(runtimeGraphService.build(89L, false)).thenReturn(graph);

        BpmApprovalStageEntity stage = new BpmApprovalStageEntity();
        stage.setApprovalStageId(31L);
        stage.setStageInvocationId("expense-review:0");
        stage.setAuthoredNodeId("expense-review");
        stage.setGeneration(0);
        stage.setStageState("APPROVED");
        stage.setTerminalReason("ALL_APPROVED");
        stage.setCompletionMode("RATIO");
        stage.setRatioPercent(67);
        stage.setEffectiveMemberCount(3);
        stage.setRequiredApprovalCount(2);
        stage.setCandidatePolicyVersionId(12L);
        stage.setApprovalPolicyVersionId(18L);
        BpmApprovalStageMemberEntity member = new BpmApprovalStageMemberEntity();
        member.setApprovalStageMemberId(41L);
        member.setMemberOrder(1);
        member.setSourceEmployeeId(1001L);
        member.setCurrentEmployeeId(1002L);
        member.setMemberState("APPROVED");
        member.setActionResult("APPROVE");
        member.setTaskId(51L);
        member.setMemberSnapshotJson("{\"displayName\":\"原审批人\"}");
        BpmTaskEntity task = new BpmTaskEntity();
        task.setAssigneeNameSnapshot("当前处理人");
        when(approvalStageDao.selectList(Mockito.any())).thenReturn(List.of(stage));
        when(approvalStageMemberDao.selectByApprovalStageId(31L)).thenReturn(List.of(member));
        when(bpmTaskDao.selectByApprovalStageMemberId(41L)).thenReturn(task);

        ResponseDTO<BpmInstanceTraceVO> response = traceService.getTrace(89L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getApprovalStages()).singleElement().satisfies(stageVO -> {
            assertThat(stageVO.getStageInvocationId()).isEqualTo("expense-review:0");
            assertThat(stageVO.getCompletionMode()).isEqualTo("RATIO");
            assertThat(stageVO.getRequiredApprovalCount()).isEqualTo(2);
            assertThat(stageVO.getApprovedMemberCount()).isEqualTo(1);
            assertThat(stageVO.getCandidatePolicyVersionId()).isEqualTo(12L);
            assertThat(stageVO.getApprovalPolicyVersionId()).isEqualTo(18L);
            assertThat(stageVO.getMembers()).singleElement().satisfies(memberVO -> {
                assertThat(memberVO.getSourceEmployeeId()).isEqualTo(1001L);
                assertThat(memberVO.getCurrentEmployeeId()).isEqualTo(1002L);
                assertThat(memberVO.getSourceEmployeeNameSnapshot()).isEqualTo("原审批人");
                assertThat(memberVO.getCurrentEmployeeNameSnapshot()).isEqualTo("当前处理人");
                assertThat(memberVO.getMemberState()).isEqualTo("APPROVED");
            });
            assertThat(stageVO.getClass().getDeclaredFields())
                    .extracting(field -> field.getName())
                    .doesNotContain("engineProcessInstanceId", "engineExecutionId", "approvalPolicySnapshotJson");
        });
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }

    private Object getField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
