package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalCompletionMode;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalPolicyDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateAutomaticOutcome;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateMember;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateSnapshot;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageMemberDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageService;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmApprovalStageServiceTest {

    @Test
    void openShouldPersistAutomaticApprovalWithoutCreatingMembers() {
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        BpmApprovalStageMemberDao memberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        BpmApprovalStageService service = service(stageDao, memberDao);
        doAnswer(invocation -> {
            BpmApprovalStageEntity stage = invocation.getArgument(0);
            stage.setApprovalStageId(81L);
            return 1;
        }).when(stageDao).insert(any(BpmApprovalStageEntity.class));

        BpmApprovalStageEntity stage = service.open(new BpmApprovalStageService.OpenApprovalStageCommand(
                11L, 1L, 101L, "finance-review", 3, "execution-auto", 31L,
                "candidate-policy-digest", 41L, "approval-policy-digest",
                new ApprovalPolicyDocument(ApprovalCompletionMode.SINGLE, 100, "IMMEDIATE"),
                new ResolvedCandidateSnapshot(List.of(), List.of(), CandidateAutomaticOutcome.AUTO_APPROVE),
                "process-91", "execution-auto"
        ));

        assertThat(stage.getStageState()).isEqualTo("APPROVED");
        assertThat(stage.getTerminalReason()).isEqualTo("AUTO_APPROVE");
        assertThat(stage.getEffectiveMemberCount()).isZero();
        assertThat(stage.getRequiredApprovalCount()).isZero();
        assertThat(stage.getClosedAt()).isNotNull();
        verify(memberDao, never()).insert(any(BpmApprovalStageMemberEntity.class));
    }

    @Test
    void openShouldFreezeMembersBySourceEmployeeIdAndActivateOnlyFirstSequentialMember() {
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        BpmApprovalStageMemberDao memberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        BpmApprovalStageService service = service(stageDao, memberDao);
        doAnswer(invocation -> {
            BpmApprovalStageEntity stage = invocation.getArgument(0);
            stage.setApprovalStageId(81L);
            return 1;
        }).when(stageDao).insert(any(BpmApprovalStageEntity.class));

        BpmApprovalStageEntity stage = service.open(command(
                "stage-finance-1",
                new ApprovalPolicyDocument(ApprovalCompletionMode.SEQUENTIAL, 100, "IMMEDIATE"),
                List.of(member(30L), member(20L))
        ));

        assertThat(stage.getApprovalStageId()).isEqualTo(81L);
        assertThat(stage.getStageInvocationId()).isEqualTo("stage-finance-1");
        assertThat(stage.getEffectiveMemberCount()).isEqualTo(2);
        assertThat(stage.getRequiredApprovalCount()).isEqualTo(2);
        assertThat(stage.getCandidateSnapshotDigest()).isNotBlank();
        assertThat(JSON.parseArray(stage.getCandidateSnapshotJson())
                .getJSONObject(0)
                .getLong("sourceEmployeeId"))
                .isEqualTo(20L);

        ArgumentCaptor<BpmApprovalStageMemberEntity> members =
                ArgumentCaptor.forClass(BpmApprovalStageMemberEntity.class);
        verify(memberDao, Mockito.times(2)).insert(members.capture());
        assertThat(members.getAllValues())
                .extracting(BpmApprovalStageMemberEntity::getSourceEmployeeId)
                .containsExactly(20L, 30L);
        assertThat(members.getAllValues())
                .extracting(BpmApprovalStageMemberEntity::getMemberOrder)
                .containsExactly(1, 2);
        assertThat(members.getAllValues())
                .extracting(BpmApprovalStageMemberEntity::getMemberState)
                .containsExactly("ACTIVE", "PLANNED");
        assertThat(members.getAllValues())
                .extracting(BpmApprovalStageMemberEntity::getCandidateSnapshotDigest)
                .containsOnly(stage.getCandidateSnapshotDigest());
    }

    @Test
    void openShouldReuseTheSameStageForTheSameInvocationWithoutDuplicatingMembers() {
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        BpmApprovalStageMemberDao memberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        BpmApprovalStageService service = service(stageDao, memberDao);
        BpmApprovalStageEntity existing = new BpmApprovalStageEntity();
        existing.setApprovalStageId(81L);
        existing.setStageInvocationId("stage-finance-1");
        when(stageDao.selectByInstanceIdAndAuthoredNodeIdAndGeneration(11L, "finance-review", 3))
                .thenReturn(existing);

        BpmApprovalStageEntity stage = service.open(command(
                "stage-finance-1",
                new ApprovalPolicyDocument(ApprovalCompletionMode.ALL, 100, "IMMEDIATE"),
                List.of(member(20L), member(30L))
        ));

        assertThat(stage).isSameAs(existing);
        verify(stageDao, never()).insert(any(BpmApprovalStageEntity.class));
        verify(memberDao, never()).insert(any(BpmApprovalStageMemberEntity.class));
    }

    @Test
    void openShouldRejectReusingAStageInvocationForAnotherStage() {
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        BpmApprovalStageMemberDao memberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        BpmApprovalStageService service = service(stageDao, memberDao);
        BpmApprovalStageEntity existing = new BpmApprovalStageEntity();
        existing.setApprovalStageId(82L);
        existing.setInstanceId(99L);
        existing.setAuthoredNodeId("other-node");
        existing.setGeneration(1);
        existing.setStageInvocationId("stage-finance-1");
        when(stageDao.selectByStageInvocationId("stage-finance-1")).thenReturn(existing);

        assertThatThrownBy(() -> service.open(command(
                "stage-finance-1",
                new ApprovalPolicyDocument(ApprovalCompletionMode.ALL, 100, "IMMEDIATE"),
                List.of(member(20L), member(30L))
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stageInvocationId");

        verify(stageDao, never()).insert(any(BpmApprovalStageEntity.class));
        verify(memberDao, never()).insert(any(BpmApprovalStageMemberEntity.class));
    }

    @Test
    void openShouldBindTheFlowableExecutionAndRemainIdempotentForTheSameExecution() {
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        BpmApprovalStageMemberDao memberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        BpmApprovalStageService service = service(stageDao, memberDao);
        AtomicReference<BpmApprovalStageEntity> stored = new AtomicReference<>();
        doAnswer(invocation -> {
            BpmApprovalStageEntity stage = invocation.getArgument(0);
            stage.setApprovalStageId(81L);
            stored.set(stage);
            return 1;
        }).when(stageDao).insert(any(BpmApprovalStageEntity.class));
        when(stageDao.selectByStageInvocationId("execution-92")).thenAnswer(invocation -> stored.get());

        BpmApprovalStageEntity first = service.open(command(
                "execution-92",
                new ApprovalPolicyDocument(ApprovalCompletionMode.SINGLE, 100, "IMMEDIATE"),
                List.of(member(20L))
        ));
        BpmApprovalStageEntity second = service.open(command(
                "execution-92",
                new ApprovalPolicyDocument(ApprovalCompletionMode.SINGLE, 100, "IMMEDIATE"),
                List.of(member(20L))
        ));

        assertThat(first.getEngineProcessInstanceId()).isEqualTo("process-91");
        assertThat(first.getEngineExecutionId()).isEqualTo("execution-92");
        assertThat(second).isSameAs(first);
        verify(stageDao, Mockito.times(1)).insert(any(BpmApprovalStageEntity.class));
    }

    @Test
    void claimEngineEffectShouldUseTheDurablePendingStateOnlyOnce() {
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        BpmApprovalStageMemberDao memberDao = Mockito.mock(BpmApprovalStageMemberDao.class);
        BpmApprovalStageService service = service(stageDao, memberDao);
        BpmApprovalStageEntity stage = new BpmApprovalStageEntity();
        stage.setApprovalStageId(81L);
        stage.setStageInvocationId("execution-92");
        stage.setEngineProcessInstanceId("process-91");
        stage.setEngineExecutionId("execution-92");
        stage.setEngineEffectState("PENDING");
        when(stageDao.selectByStageInvocationId("execution-92")).thenReturn(stage);
        when(stageDao.claimEngineEffect(81L, "APPROVED")).thenReturn(1);

        BpmApprovalStageService.EngineEffectClaim claim = service.claimEngineEffect("execution-92", "APPROVED");

        assertThat(claim.stageInvocationId()).isEqualTo("execution-92");
        assertThat(claim.engineProcessInstanceId()).isEqualTo("process-91");
        assertThat(claim.engineExecutionId()).isEqualTo("execution-92");
        verify(stageDao).claimEngineEffect(81L, "APPROVED");
    }

    @Test
    void engineEffectLifecycleShouldAlwaysUseIndependentTransactions() throws NoSuchMethodException {
        assertRequiresNew("claimEngineEffect", String.class, String.class);
        assertRequiresNew("markEngineEffectCompleted", Long.class);
        assertRequiresNew("markEngineEffectFailed", Long.class, String.class);
    }

    private void assertRequiresNew(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = BpmApprovalStageService.class.getMethod(methodName, parameterTypes);
        Transactional transaction = method.getAnnotation(Transactional.class);

        assertThat(transaction).isNotNull();
        assertThat(transaction.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    private BpmApprovalStageService service(
            BpmApprovalStageDao stageDao,
            BpmApprovalStageMemberDao memberDao
    ) {
        BpmApprovalStageService service = new BpmApprovalStageService();
        setField(service, "bpmApprovalStageDao", stageDao);
        setField(service, "bpmApprovalStageMemberDao", memberDao);
        return service;
    }

    private BpmApprovalStageService.OpenApprovalStageCommand command(
            String stageInvocationId,
            ApprovalPolicyDocument policy,
            List<ResolvedCandidateMember> members
    ) {
        return new BpmApprovalStageService.OpenApprovalStageCommand(
                11L,
                1L,
                101L,
                "finance-review",
                3,
                stageInvocationId,
                31L,
                "candidate-policy-digest",
                41L,
                "approval-policy-digest",
                policy,
                new ResolvedCandidateSnapshot(members, List.of()),
                "process-91",
                stageInvocationId
        );
    }

    private ResolvedCandidateMember member(Long employeeId) {
        return new ResolvedCandidateMember(
                employeeId,
                employeeId,
                "员工" + employeeId,
                7L,
                "财务部"
        );
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
