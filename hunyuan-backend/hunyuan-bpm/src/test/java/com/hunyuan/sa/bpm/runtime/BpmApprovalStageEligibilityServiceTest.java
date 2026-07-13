package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageMemberDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageEligibilityService;
import com.hunyuan.sa.bpm.module.candidate.service.ApprovalCompletionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmApprovalStageEligibilityServiceTest {

    @Test
    void invalidCurrentEmployeeShouldBecomeIneligibleAndPauseTheStage() {
        BpmApprovalStageMemberDao memberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
        BpmTaskActionLogDao logDao = Mockito.mock(BpmTaskActionLogDao.class);
        BpmOrgIdentityGateway identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        BpmApprovalStageEligibilityService service = new BpmApprovalStageEligibilityService();
        setField(service, "bpmApprovalStageMemberDao", memberDao);
        setField(service, "bpmApprovalStageDao", stageDao);
        setField(service, "bpmTaskDao", taskDao);
        setField(service, "bpmTaskActionLogDao", logDao);
        setField(service, "bpmOrgIdentityGateway", identityGateway);
        setField(service, "approvalCompletionService", new ApprovalCompletionService());
        setField(service, "applicationEventPublisher", Mockito.mock(ApplicationEventPublisher.class));

        BpmApprovalStageMemberEntity member = new BpmApprovalStageMemberEntity();
        member.setApprovalStageMemberId(11L);
        member.setApprovalStageId(1L);
        member.setCurrentEmployeeId(20L);
        member.setSourceEmployeeId(20L);
        member.setMemberOrder(1);
        member.setMemberState("ACTIVE");
        BpmApprovalStageEntity stage = new BpmApprovalStageEntity();
        stage.setApprovalStageId(1L);
        stage.setStageState("ACTIVE");
        stage.setStageInvocationId("stage-1");
        stage.setTenantId(1L);
        stage.setCompletionMode("SINGLE");
        stage.setRatioPercent(100);
        stage.setRejectionRule("IMMEDIATE");
        stage.setApprovalPolicySnapshotJson("{\"allowedActions\":[\"APPROVE\",\"REJECT\",\"RETURN\"]}");
        stage.setRevision(0);
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(101L);
        task.setInstanceId(8L);
        task.setTaskState(1);
        task.setDefinitionSource("GRAPH");
        when(memberDao.selectByIdForUpdate(11L)).thenReturn(member);
        when(memberDao.updateById(Mockito.any(BpmApprovalStageMemberEntity.class))).thenReturn(1);
        when(stageDao.selectByIdForUpdate(1L)).thenReturn(stage);
        when(memberDao.selectByApprovalStageIdForUpdate(1L)).thenReturn(java.util.List.of(member));
        when(taskDao.selectByApprovalStageMemberIdForUpdate(11L)).thenReturn(task);
        when(taskDao.updateById(Mockito.any(BpmTaskEntity.class))).thenReturn(1);
        when(identityGateway.requireEmployee(20L)).thenThrow(new IllegalArgumentException("员工已禁用"));

        assertThat(service.reconcile(11L)).isTrue();

        ArgumentCaptor<BpmApprovalStageMemberEntity> memberCaptor = ArgumentCaptor.forClass(BpmApprovalStageMemberEntity.class);
        verify(memberDao).updateById(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getMemberState()).isEqualTo("INELIGIBLE");
        verify(stageDao).markMemberEligibilityException(1L, "MEMBER_INELIGIBLE");
        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(logDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("M2_MEMBER_INELIGIBLE");
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
