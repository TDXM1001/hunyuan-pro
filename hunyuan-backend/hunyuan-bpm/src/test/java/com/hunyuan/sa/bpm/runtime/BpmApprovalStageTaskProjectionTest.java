package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberState;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageMemberDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmApprovalStageTaskProjectionTest {

    @Test
    void projectActiveMembersShouldCreateOneM2TaskPerActiveMemberWithoutAnEngineTask() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
        BpmApprovalStageMemberDao memberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        BpmOrgIdentityGateway identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        BpmTaskProjectionService service = service(instanceDao, taskDao, memberDao, identityGateway);

        BpmApprovalStageEntity stage = stage(70L, "ALL");
        BpmApprovalStageMemberEntity first = member(501L, 20L, ApprovalMemberState.ACTIVE);
        BpmApprovalStageMemberEntity second = member(502L, 30L, ApprovalMemberState.ACTIVE);
        when(instanceDao.selectById(8L)).thenReturn(instance());
        when(memberDao.selectByApprovalStageId(70L)).thenReturn(List.of(first, second));
        when(taskDao.selectByApprovalStageMemberId(501L)).thenReturn(null);
        when(taskDao.selectByApprovalStageMemberId(502L)).thenReturn(null);
        when(identityGateway.requireEmployee(20L)).thenReturn(employee(20L));
        when(identityGateway.requireEmployee(30L)).thenReturn(employee(30L));
        AtomicLong taskIds = new AtomicLong(900L);
        when(taskDao.insert(any(BpmTaskEntity.class))).thenAnswer(invocation -> {
            BpmTaskEntity task = invocation.getArgument(0);
            task.setTaskId(taskIds.getAndIncrement());
            return 1;
        });

        int created = service.projectActiveApprovalStageMembers(stage);

        assertThat(created).isEqualTo(2);
        ArgumentCaptor<BpmTaskEntity> tasks = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(taskDao, times(2)).insert(tasks.capture());
        assertThat(tasks.getAllValues())
                .extracting(BpmTaskEntity::getApprovalStageId)
                .containsOnly(70L);
        assertThat(tasks.getAllValues())
                .extracting(BpmTaskEntity::getApprovalStageMemberId)
                .containsExactly(501L, 502L);
        assertThat(tasks.getAllValues())
                .extracting(BpmTaskEntity::getEngineTaskId)
                .containsOnlyNulls();
        assertThat(tasks.getAllValues())
                .extracting(BpmTaskEntity::getEngineExecutionId)
                .containsOnly("execution-91");
        verify(memberDao, times(2)).updateById(any(BpmApprovalStageMemberEntity.class));
    }

    @Test
    void projectActiveMembersShouldLeavePlannedSequentialMembersWithoutTasks() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
        BpmApprovalStageMemberDao memberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        BpmOrgIdentityGateway identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        BpmTaskProjectionService service = service(instanceDao, taskDao, memberDao, identityGateway);

        BpmApprovalStageEntity stage = stage(70L, "SEQUENTIAL");
        BpmApprovalStageMemberEntity first = member(501L, 20L, ApprovalMemberState.ACTIVE);
        BpmApprovalStageMemberEntity second = member(502L, 30L, ApprovalMemberState.PLANNED);
        when(instanceDao.selectById(8L)).thenReturn(instance());
        when(memberDao.selectByApprovalStageId(70L)).thenReturn(List.of(first, second));
        when(taskDao.selectByApprovalStageMemberId(501L)).thenReturn(null);
        when(identityGateway.requireEmployee(20L)).thenReturn(employee(20L));
        when(taskDao.insert(any(BpmTaskEntity.class))).thenAnswer(invocation -> {
            invocation.<BpmTaskEntity>getArgument(0).setTaskId(900L);
            return 1;
        });

        int created = service.projectActiveApprovalStageMembers(stage);

        assertThat(created).isEqualTo(1);
        verify(taskDao).insert(any(BpmTaskEntity.class));
        verify(taskDao, never()).selectByApprovalStageMemberId(502L);
    }

    @Test
    void projectActiveMembersShouldReuseConcurrentMemberTaskAfterDuplicateKey() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
        BpmApprovalStageMemberDao memberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        BpmOrgIdentityGateway identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        BpmTaskProjectionService service = service(instanceDao, taskDao, memberDao, identityGateway);

        BpmApprovalStageMemberEntity member = member(501L, 20L, ApprovalMemberState.ACTIVE);
        BpmTaskEntity concurrentTask = new BpmTaskEntity();
        concurrentTask.setTaskId(901L);
        when(instanceDao.selectById(8L)).thenReturn(instance());
        when(memberDao.selectByApprovalStageId(70L)).thenReturn(List.of(member));
        when(taskDao.selectByApprovalStageMemberId(501L)).thenReturn(null, concurrentTask);
        when(identityGateway.requireEmployee(20L)).thenReturn(employee(20L));
        when(taskDao.insert(any(BpmTaskEntity.class))).thenThrow(new DuplicateKeyException("duplicate stage member"));

        int created = service.projectActiveApprovalStageMembers(stage(70L, "ALL"));

        assertThat(created).isZero();
        verify(memberDao).updateById(any(BpmApprovalStageMemberEntity.class));
    }

    private BpmTaskProjectionService service(
            BpmInstanceDao instanceDao,
            BpmTaskDao taskDao,
            BpmApprovalStageMemberDao memberDao,
            BpmOrgIdentityGateway identityGateway
    ) {
        BpmTaskProjectionService service = new BpmTaskProjectionService();
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmTaskDao", taskDao);
        setField(service, "bpmApprovalStageMemberDao", memberDao);
        setField(service, "bpmOrgIdentityGateway", identityGateway);
        return service;
    }

    private BpmApprovalStageEntity stage(Long stageId, String completionMode) {
        BpmApprovalStageEntity stage = new BpmApprovalStageEntity();
        stage.setApprovalStageId(stageId);
        stage.setInstanceId(8L);
        stage.setAuthoredNodeId("finance-review");
        stage.setCompletionMode(completionMode);
        stage.setEngineProcessInstanceId("process-91");
        stage.setEngineExecutionId("execution-91");
        return stage;
    }

    private BpmApprovalStageMemberEntity member(Long memberId, Long employeeId, ApprovalMemberState state) {
        BpmApprovalStageMemberEntity member = new BpmApprovalStageMemberEntity();
        member.setApprovalStageMemberId(memberId);
        member.setCurrentEmployeeId(employeeId);
        member.setMemberState(state.name());
        return member;
    }

    private BpmInstanceEntity instance() {
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setDefinitionId(9L);
        instance.setInstanceNo("SN-2026-0001");
        instance.setTitle("报销申请");
        instance.setStartEmployeeId(7L);
        instance.setStartEmployeeNameSnapshot("申请人");
        instance.setCategoryIdSnapshot(3L);
        instance.setCategoryNameSnapshot("费用流程");
        return instance;
    }

    private BpmEmployeeSnapshot employee(Long employeeId) {
        return new BpmEmployeeSnapshot(employeeId, "审批人" + employeeId, 3L, "财务部", null, null);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
