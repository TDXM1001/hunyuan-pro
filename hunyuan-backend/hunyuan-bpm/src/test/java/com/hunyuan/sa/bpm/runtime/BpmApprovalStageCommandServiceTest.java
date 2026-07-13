package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.model.EngineEffect;
import com.hunyuan.sa.bpm.module.candidate.service.ApprovalCompletionService;
import com.hunyuan.sa.bpm.module.candidate.service.ParticipantAuthorizationService;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalCommandReceiptDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageMemberDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalCommandReceiptEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.event.BpmApprovalStageEngineEffectRequestedEvent;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageCommandService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmApprovalStageCommandServiceTest {

    private BpmApprovalStageCommandService service;
    private BpmTaskDao taskDao;
    private BpmApprovalStageMemberDao memberDao;
    private BpmTaskProjectionService projectionService;
    private ApplicationEventPublisher eventPublisher;

    private BpmApprovalCommandReceiptDao receiptDao;

    @BeforeEach
    void setUp() {
        service = new BpmApprovalStageCommandService();
        taskDao = Mockito.mock(BpmTaskDao.class);
        memberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        projectionService = Mockito.mock(BpmTaskProjectionService.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        receiptDao = Mockito.mock(BpmApprovalCommandReceiptDao.class);
        when(receiptDao.updateById(any(BpmApprovalCommandReceiptEntity.class))).thenReturn(1);
        setField(service, "bpmTaskDao", taskDao);
        setField(service, "bpmInstanceDao", instanceDao());
        setField(service, "bpmApprovalStageDao", Mockito.mock(BpmApprovalStageDao.class));
        setField(service, "bpmApprovalStageMemberDao", memberDao);
        setField(service, "bpmCurrentActorProvider", currentActorProvider());
        setField(service, "bpmOrgIdentityGateway", identityGateway());
        setField(service, "participantAuthorizationService", new ParticipantAuthorizationService());
        setField(service, "approvalCompletionService", new ApprovalCompletionService());
        setField(service, "applicationEventPublisher", eventPublisher);
        setField(service, "bpmTaskProjectionService", projectionService);
        setField(service, "bpmTaskActionLogDao", Mockito.mock(BpmTaskActionLogDao.class));
        setField(service, "bpmApprovalCommandReceiptDao", receiptDao);
    }

    @Test
    void sequentialApprovalShouldActivateOnlyNextFrozenMember() {
        BpmTaskEntity task = task(101L, 11L);
        BpmApprovalStageEntity stage = stage("SEQUENTIAL");
        BpmApprovalStageMemberEntity first = member(11L, 1, "ACTIVE", 20L);
        BpmApprovalStageMemberEntity second = member(12L, 2, "PLANNED", 30L);
        arrange(task, stage, List.of(first, second));

        ResponseDTO<String> response = service.execute(101L, "APPROVE", "同意");

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmApprovalStageMemberEntity> memberCaptor = ArgumentCaptor.forClass(BpmApprovalStageMemberEntity.class);
        verify(memberDao, org.mockito.Mockito.times(2)).updateById(memberCaptor.capture());
        assertThat(memberCaptor.getAllValues())
                .extracting(BpmApprovalStageMemberEntity::getMemberState)
                .containsExactly("APPROVED", "ACTIVE");
        verify(projectionService).projectActiveApprovalStageMembers(stage);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void anyApprovalShouldCompleteControlOnceAndTerminateRemainingMember() {
        BpmTaskEntity task = task(101L, 11L);
        BpmApprovalStageEntity stage = stage("ANY");
        BpmApprovalStageMemberEntity first = member(11L, 1, "ACTIVE", 20L);
        BpmApprovalStageMemberEntity second = member(12L, 2, "ACTIVE", 30L);
        BpmTaskEntity secondTask = task(102L, 12L);
        arrange(task, stage, List.of(first, second));
        when(taskDao.selectByApprovalStageMemberId(12L)).thenReturn(secondTask);

        ResponseDTO<String> response = service.execute(101L, "APPROVE", "同意");

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmApprovalStageEngineEffectRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(BpmApprovalStageEngineEffectRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().stageInvocationId()).isEqualTo("execution-1");
        assertThat(eventCaptor.getValue().engineEffect()).isEqualTo(EngineEffect.COMPLETE_ONCE);
        assertThat(eventCaptor.getValue().terminalReason()).isEqualTo("APPROVED");
        ArgumentCaptor<com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity> actionLogCaptor =
                ArgumentCaptor.forClass(com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity.class);
        verify(getField(service, "bpmTaskActionLogDao", BpmTaskActionLogDao.class)).insert(actionLogCaptor.capture());
        assertThat(actionLogCaptor.getValue().getDefinitionId()).isNull();
        assertThat(actionLogCaptor.getValue().getGraphDefinitionVersionId()).isEqualTo(41L);
        assertThat(actionLogCaptor.getValue().getDefinitionSource()).isEqualTo("GRAPH");
        verify(taskDao).updateById(org.mockito.ArgumentMatchers.argThat((BpmTaskEntity updated) ->
                updated.getTaskId().equals(102L) && updated.getCancelledAt() != null
        ));
    }

    @Test
    void rejectionShouldPersistTerminalFactsBeforeQueuingTheCloseEffect() {
        BpmTaskEntity task = task(101L, 11L);
        BpmApprovalStageEntity stage = stage("ALL");
        BpmApprovalStageMemberEntity first = member(11L, 1, "ACTIVE", 20L);
        BpmApprovalStageMemberEntity second = member(12L, 2, "ACTIVE", 30L);
        arrange(task, stage, List.of(first, second));

        ResponseDTO<String> response = service.execute(101L, "REJECT", "不同意");

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmApprovalStageEngineEffectRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(BpmApprovalStageEngineEffectRequestedEvent.class);
        InOrder order = Mockito.inOrder(getField(service, "bpmApprovalStageDao", BpmApprovalStageDao.class), eventPublisher);
        order.verify(getField(service, "bpmApprovalStageDao", BpmApprovalStageDao.class))
                .updateState(any(), any(), any(), any(), any());
        order.verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().engineEffect()).isEqualTo(EngineEffect.CLOSE_ONCE);
        assertThat(eventCaptor.getValue().terminalReason()).isEqualTo("REJECTED");
    }

    @Test
    void actionOutsideFrozenPolicyShouldBeRejectedBeforeChangingStageFacts() {
        BpmTaskEntity task = task(101L, 11L);
        BpmApprovalStageEntity stage = stage("SINGLE");
        stage.setApprovalPolicySnapshotJson("""
                {"completionMode":"SINGLE","ratioPercent":100,"rejectionRule":"IMMEDIATE","allowedActions":["APPROVE"]}
                """);
        BpmApprovalStageMemberEntity member = member(11L, 1, "ACTIVE", 20L);
        arrange(task, stage, List.of(member));

        ResponseDTO<String> response = service.execute(101L, "REJECT", "不同意");

        assertThat(response.getOk()).isFalse();
        verify(memberDao, never()).updateById(any(BpmApprovalStageMemberEntity.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void duplicateRequestShouldReturnStoredSuccessWithoutApplyingTheActionTwice() {
        BpmTaskEntity task = task(101L, 11L);
        BpmApprovalStageEntity stage = stage("ANY");
        BpmApprovalStageMemberEntity first = member(11L, 1, "ACTIVE", 20L);
        BpmApprovalStageMemberEntity second = member(12L, 2, "ACTIVE", 30L);
        arrange(task, stage, List.of(first, second));
        when(taskDao.selectByApprovalStageMemberId(12L)).thenReturn(task(102L, 12L));
        java.util.concurrent.atomic.AtomicReference<BpmApprovalCommandReceiptEntity> stored =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(receiptDao.selectForUpdate(1L, 8L, "request-1"))
                .thenAnswer(invocation -> stored.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            BpmApprovalCommandReceiptEntity receipt = invocation.getArgument(0);
            receipt.setApprovalCommandReceiptId(91L);
            stored.set(receipt);
            return 1;
        }).when(receiptDao).insert(any(BpmApprovalCommandReceiptEntity.class));
        when(receiptDao.updateById(any(BpmApprovalCommandReceiptEntity.class))).thenReturn(1);

        ResponseDTO<String> firstResponse = service.execute(101L, "APPROVE", "同意", "request-1");
        ResponseDTO<String> replayResponse = service.execute(101L, "APPROVE", "同意", "request-1");

        assertThat(firstResponse.getOk()).isTrue();
        assertThat(replayResponse.getOk()).isTrue();
        verify(eventPublisher, Mockito.times(1)).publishEvent(
                any(BpmApprovalStageEngineEffectRequestedEvent.class)
        );
        verify(memberDao, Mockito.times(2)).updateById(any(BpmApprovalStageMemberEntity.class));
    }

    @Test
    void concurrentReceiptOwnerShouldBeTheOnlyCallerAllowedToApplyTheAction() {
        BpmTaskEntity task = task(101L, 11L);
        BpmApprovalStageEntity stage = stage("SINGLE");
        BpmApprovalStageMemberEntity member = member(11L, 1, "ACTIVE", 20L);
        arrange(task, stage, List.of(member));
        java.util.concurrent.atomic.AtomicReference<BpmApprovalCommandReceiptEntity> concurrent =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(receiptDao.selectForUpdate(1L, 8L, "request-race"))
                .thenAnswer(invocation -> concurrent.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            concurrent.set(invocation.getArgument(0));
            throw new org.springframework.dao.DuplicateKeyException("concurrent request");
        }).when(receiptDao).insert(any(BpmApprovalCommandReceiptEntity.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.execute(101L, "APPROVE", "同意", "request-race")
                )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("尚未完成");

        verify(memberDao, never()).updateById(any(BpmApprovalStageMemberEntity.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    private void arrange(
            BpmTaskEntity task,
            BpmApprovalStageEntity stage,
            List<BpmApprovalStageMemberEntity> members
    ) {
        BpmApprovalStageDao stageDao = getField(service, "bpmApprovalStageDao", BpmApprovalStageDao.class);
        when(taskDao.selectById(101L)).thenReturn(task);
        when(taskDao.selectByIdForUpdate(101L)).thenReturn(task);
        when(stageDao.selectByIdForUpdate(1L)).thenReturn(stage);
        when(stageDao.updateState(any(), any(), any(), any(), any())).thenReturn(1);
        when(memberDao.selectByApprovalStageIdForUpdate(1L)).thenReturn(members);
        when(memberDao.updateById(any(BpmApprovalStageMemberEntity.class))).thenReturn(1);
    }

    private BpmTaskEntity task(Long taskId, Long memberId) {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(taskId);
        task.setInstanceId(8L);
        task.setApprovalStageId(1L);
        task.setApprovalStageMemberId(memberId);
        task.setAssigneeEmployeeId(memberId.equals(11L) ? 20L : 30L);
        task.setTaskState(1);
        task.setDefinitionSource("GRAPH");
        task.setGraphDefinitionVersionId(41L);
        return task;
    }

    private BpmApprovalStageEntity stage(String completionMode) {
        BpmApprovalStageEntity stage = new BpmApprovalStageEntity();
        stage.setApprovalStageId(1L);
        stage.setInstanceId(8L);
        stage.setTenantId(1L);
        stage.setStageInvocationId("execution-1");
        stage.setStageState("ACTIVE");
        stage.setCompletionMode(completionMode);
        stage.setRatioPercent(100);
        stage.setRejectionRule("IMMEDIATE");
        stage.setEffectiveMemberCount(2);
        stage.setRequiredApprovalCount("ANY".equals(completionMode) ? 1 : 2);
        stage.setApprovalPolicySnapshotJson("{\"completionMode\":\"" + completionMode
                + "\",\"ratioPercent\":100,\"rejectionRule\":\"IMMEDIATE\",\"allowedActions\":[\"APPROVE\",\"REJECT\",\"RETURN\"]}");
        stage.setRevision(0);
        return stage;
    }

    private BpmApprovalStageMemberEntity member(Long memberId, int order, String state, Long employeeId) {
        BpmApprovalStageMemberEntity member = new BpmApprovalStageMemberEntity();
        member.setApprovalStageMemberId(memberId);
        member.setApprovalStageId(1L);
        member.setMemberOrder(order);
        member.setSourceEmployeeId(employeeId);
        member.setCurrentEmployeeId(employeeId);
        member.setMemberState(state);
        return member;
    }

    private BpmInstanceDao instanceDao() {
        BpmInstanceDao dao = Mockito.mock(BpmInstanceDao.class);
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setRunState(1);
        when(dao.selectByIdForUpdate(8L)).thenReturn(instance);
        return dao;
    }

    private BpmCurrentActorProvider currentActorProvider() {
        BpmCurrentActorProvider provider = Mockito.mock(BpmCurrentActorProvider.class);
        when(provider.requireCurrentEmployeeId()).thenReturn(20L);
        return provider;
    }

    private BpmOrgIdentityGateway identityGateway() {
        BpmOrgIdentityGateway gateway = Mockito.mock(BpmOrgIdentityGateway.class);
        when(gateway.requireEmployee(20L)).thenReturn(new BpmEmployeeSnapshot(20L, "审批人", 7L, "财务部", null, null));
        return gateway;
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

    private static <T> T getField(Object target, String fieldName, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
