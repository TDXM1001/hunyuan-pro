package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageMemberDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageGovernanceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmApprovalStageGovernanceServiceTest {

    @Test
    void controlledTransferShouldPreserveSourceMemberAndFrozenDenominator() {
        BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        BpmApprovalStageMemberDao memberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        BpmTaskActionLogDao logDao = Mockito.mock(BpmTaskActionLogDao.class);
        BpmCurrentActorProvider actorProvider = Mockito.mock(BpmCurrentActorProvider.class);
        BpmOrgIdentityGateway identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        BpmApprovalStageGovernanceService service = new BpmApprovalStageGovernanceService();
        setField(service, "bpmTaskDao", taskDao);
        setField(service, "bpmApprovalStageDao", stageDao);
        setField(service, "bpmApprovalStageMemberDao", memberDao);
        setField(service, "bpmTaskActionLogDao", logDao);
        setField(service, "bpmCurrentActorProvider", actorProvider);
        setField(service, "bpmOrgIdentityGateway", identityGateway);

        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(101L);
        task.setInstanceId(8L);
        task.setApprovalStageId(1L);
        task.setApprovalStageMemberId(11L);
        task.setAssigneeEmployeeId(20L);
        task.setTaskState(1);
        task.setDefinitionSource("GRAPH");
        task.setGraphDefinitionVersionId(41L);
        BpmApprovalStageEntity stage = new BpmApprovalStageEntity();
        stage.setApprovalStageId(1L);
        stage.setInstanceId(8L);
        stage.setStageState("ACTIVE");
        stage.setEffectiveMemberCount(3);
        stage.setRequiredApprovalCount(2);
        BpmApprovalStageMemberEntity member = new BpmApprovalStageMemberEntity();
        member.setApprovalStageMemberId(11L);
        member.setApprovalStageId(1L);
        member.setSourceEmployeeId(20L);
        member.setCurrentEmployeeId(20L);
        member.setMemberState("ACTIVE");
        when(taskDao.selectByIdForUpdate(101L)).thenReturn(task);
        when(stageDao.selectByIdForUpdate(1L)).thenReturn(stage);
        when(memberDao.selectByApprovalStageIdForUpdate(1L)).thenReturn(List.of(member));
        when(actorProvider.requireCurrentEmployeeId()).thenReturn(900L);
        when(identityGateway.requireEmployee(900L)).thenReturn(new BpmEmployeeSnapshot(900L, "管理员", 1L, "平台部", null, null));
        when(identityGateway.requireEmployee(30L)).thenReturn(new BpmEmployeeSnapshot(30L, "新审批人", 7L, "财务部", null, null));
        when(memberDao.updateById(Mockito.any(BpmApprovalStageMemberEntity.class))).thenReturn(1);
        when(taskDao.updateById(Mockito.any(BpmTaskEntity.class))).thenReturn(1);

        ResponseDTO<String> response = service.transfer(101L, 30L, "原审批人离职");

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmApprovalStageMemberEntity> memberCaptor = ArgumentCaptor.forClass(BpmApprovalStageMemberEntity.class);
        verify(memberDao).updateById(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getApprovalStageMemberId()).isEqualTo(11L);
        assertThat(memberCaptor.getValue().getCurrentEmployeeId()).isEqualTo(30L);
        assertThat(memberCaptor.getValue().getSourceEmployeeId()).isNull();
        assertThat(stage.getEffectiveMemberCount()).isEqualTo(3);
        assertThat(stage.getRequiredApprovalCount()).isEqualTo(2);
        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(logDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("M2_MEMBER_TRANSFERRED");
        assertThat(logCaptor.getValue().getActorEmployeeId()).isEqualTo(900L);
        assertThat(logCaptor.getValue().getFromAssigneeEmployeeId()).isEqualTo(20L);
        assertThat(logCaptor.getValue().getToAssigneeEmployeeId()).isEqualTo(30L);
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
