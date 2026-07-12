package com.hunyuan.sa.bpm.module.candidate.service;

import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalCompletionDecision;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalCompletionMode;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberAction;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberFact;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberState;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalPolicyDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalStageFact;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalStageState;
import com.hunyuan.sa.bpm.module.candidate.domain.model.EngineEffect;
import com.hunyuan.sa.bpm.module.candidate.domain.model.MemberUpdate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ApprovalCompletionService {

    public ApprovalCompletionDecision decide(
            ApprovalPolicyDocument policy,
            ApprovalStageFact stage,
            List<ApprovalMemberFact> members,
            ApprovalMemberAction action
    ) {
        if (policy == null || stage == null || members == null || action == null) {
            throw new IllegalArgumentException("审批完成决策输入不能为空");
        }
        if (stage.state() != ApprovalStageState.ACTIVE) {
            throw new IllegalStateException("审批阶段不处于可处理状态");
        }
        ApprovalMemberFact actedMember = members.stream()
                .filter(member -> action.memberId().equals(member.memberId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("审批成员不存在"));
        if (actedMember.state() != ApprovalMemberState.ACTIVE) {
            throw new IllegalStateException("审批成员不处于可处理状态");
        }
        ApprovalMemberState actionState = actionState(action.action());
        List<MemberUpdate> updates = new ArrayList<>();
        updates.add(new MemberUpdate(actedMember.memberId(), actionState));
        if (actionState == ApprovalMemberState.REJECTED) {
            return rejectionDecision(policy, members, actedMember, updates);
        }
        if (actionState == ApprovalMemberState.RETURNED) {
            return terminalDecision(ApprovalStageState.RETURNED, members, actedMember, updates);
        }
        if (actionState == ApprovalMemberState.INELIGIBLE) {
            return unavailableMemberDecision(policy, members, actedMember, updates);
        }
        return switch (policy.completionMode()) {
            case SINGLE -> singleDecision(members, updates);
            case SEQUENTIAL -> sequentialDecision(members, actedMember, updates);
            case ALL -> allDecision(members, actedMember, updates);
            case ANY -> successfulDecision(members, actedMember, updates);
            case RATIO -> ratioDecision(policy, members, actedMember, updates);
        };
    }

    private ApprovalCompletionDecision singleDecision(List<ApprovalMemberFact> members, List<MemberUpdate> updates) {
        if (members.size() != 1) {
            throw new IllegalArgumentException("SINGLE 审批必须且只能有一个成员");
        }
        return new ApprovalCompletionDecision(ApprovalStageState.APPROVED, updates, EngineEffect.COMPLETE_ONCE);
    }

    private ApprovalCompletionDecision sequentialDecision(
            List<ApprovalMemberFact> members,
            ApprovalMemberFact actedMember,
            List<MemberUpdate> updates
    ) {
        ApprovalMemberFact nextMember = members.stream()
                .filter(member -> member.memberOrder() > actedMember.memberOrder())
                .filter(member -> member.state() == ApprovalMemberState.PLANNED)
                .min(Comparator.comparingInt(ApprovalMemberFact::memberOrder))
                .orElse(null);
        if (nextMember == null) {
            return new ApprovalCompletionDecision(ApprovalStageState.APPROVED, updates, EngineEffect.COMPLETE_ONCE);
        }
        updates.add(new MemberUpdate(nextMember.memberId(), ApprovalMemberState.ACTIVE));
        return new ApprovalCompletionDecision(ApprovalStageState.ACTIVE, updates, EngineEffect.NONE);
    }

    private ApprovalCompletionDecision allDecision(
            List<ApprovalMemberFact> members,
            ApprovalMemberFact actedMember,
            List<MemberUpdate> updates
    ) {
        boolean allApproved = members.stream().allMatch(member -> member == actedMember
                || member.state() == ApprovalMemberState.APPROVED);
        return allApproved
                ? new ApprovalCompletionDecision(ApprovalStageState.APPROVED, updates, EngineEffect.COMPLETE_ONCE)
                : new ApprovalCompletionDecision(ApprovalStageState.ACTIVE, updates, EngineEffect.NONE);
    }

    private ApprovalCompletionDecision ratioDecision(
            ApprovalPolicyDocument policy,
            List<ApprovalMemberFact> members,
            ApprovalMemberFact actedMember,
            List<MemberUpdate> updates
    ) {
        int requiredApprovalCount = (members.size() * policy.ratioPercent() + 99) / 100;
        long approvedCount = members.stream()
                .filter(member -> member == actedMember || member.state() == ApprovalMemberState.APPROVED)
                .count();
        return approvedCount >= requiredApprovalCount
                ? successfulDecision(members, actedMember, updates)
                : new ApprovalCompletionDecision(ApprovalStageState.ACTIVE, updates, EngineEffect.NONE);
    }

    private ApprovalCompletionDecision unavailableMemberDecision(
            ApprovalPolicyDocument policy,
            List<ApprovalMemberFact> members,
            ApprovalMemberFact actedMember,
            List<MemberUpdate> updates
    ) {
        return switch (policy.completionMode()) {
            case SINGLE, SEQUENTIAL, ALL -> new ApprovalCompletionDecision(
                    ApprovalStageState.EXCEPTION_PENDING,
                    updates,
                    EngineEffect.NONE
            );
            case ANY, RATIO -> reachableAfterUnavailableMember(policy, members, actedMember, updates);
        };
    }

    private ApprovalCompletionDecision reachableAfterUnavailableMember(
            ApprovalPolicyDocument policy,
            List<ApprovalMemberFact> members,
            ApprovalMemberFact actedMember,
            List<MemberUpdate> updates
    ) {
        int requiredApprovalCount = policy.completionMode() == ApprovalCompletionMode.ANY
                ? 1
                : (members.size() * policy.ratioPercent() + 99) / 100;
        long approvedCount = members.stream()
                .filter(member -> member != actedMember && member.state() == ApprovalMemberState.APPROVED)
                .count();
        long stillEligibleCount = members.stream()
                .filter(member -> member != actedMember)
                .filter(member -> member.state() == ApprovalMemberState.ACTIVE || member.state() == ApprovalMemberState.PLANNED)
                .count();
        return approvedCount + stillEligibleCount < requiredApprovalCount
                ? terminalDecision(ApprovalStageState.REJECTED, members, actedMember, updates)
                : new ApprovalCompletionDecision(ApprovalStageState.ACTIVE, updates, EngineEffect.NONE);
    }

    private ApprovalCompletionDecision rejectionDecision(
            ApprovalPolicyDocument policy,
            List<ApprovalMemberFact> members,
            ApprovalMemberFact actedMember,
            List<MemberUpdate> updates
    ) {
        boolean deferUntilUnreachable = "WHEN_APPROVAL_UNREACHABLE".equals(policy.rejectionRule())
                && (policy.completionMode() == ApprovalCompletionMode.ANY
                || policy.completionMode() == ApprovalCompletionMode.RATIO);
        if (!deferUntilUnreachable) {
            return terminalDecision(ApprovalStageState.REJECTED, members, actedMember, updates);
        }
        int requiredApprovalCount = policy.completionMode() == ApprovalCompletionMode.ANY
                ? 1
                : (members.size() * policy.ratioPercent() + 99) / 100;
        long approvedCount = members.stream()
                .filter(member -> member.state() == ApprovalMemberState.APPROVED)
                .count();
        long stillEligibleCount = members.stream()
                .filter(member -> member != actedMember)
                .filter(member -> member.state() == ApprovalMemberState.ACTIVE || member.state() == ApprovalMemberState.PLANNED)
                .count();
        return approvedCount + stillEligibleCount < requiredApprovalCount
                ? terminalDecision(ApprovalStageState.REJECTED, members, actedMember, updates)
                : new ApprovalCompletionDecision(ApprovalStageState.ACTIVE, updates, EngineEffect.NONE);
    }

    private ApprovalCompletionDecision successfulDecision(
            List<ApprovalMemberFact> members,
            ApprovalMemberFact actedMember,
            List<MemberUpdate> updates
    ) {
        appendTerminationUpdates(members, actedMember, updates);
        return new ApprovalCompletionDecision(ApprovalStageState.APPROVED, updates, EngineEffect.COMPLETE_ONCE);
    }

    private ApprovalCompletionDecision terminalDecision(
            ApprovalStageState terminalState,
            List<ApprovalMemberFact> members,
            ApprovalMemberFact actedMember,
            List<MemberUpdate> updates
    ) {
        appendTerminationUpdates(members, actedMember, updates);
        return new ApprovalCompletionDecision(terminalState, updates, EngineEffect.CLOSE_ONCE);
    }

    private void appendTerminationUpdates(
            List<ApprovalMemberFact> members,
            ApprovalMemberFact actedMember,
            List<MemberUpdate> updates
    ) {
        members.stream()
                .filter(member -> member != actedMember)
                .filter(member -> member.state() == ApprovalMemberState.ACTIVE || member.state() == ApprovalMemberState.PLANNED)
                .sorted(Comparator.comparingInt(ApprovalMemberFact::memberOrder))
                .forEach(member -> updates.add(new MemberUpdate(member.memberId(), ApprovalMemberState.TERMINATED)));
    }

    private ApprovalMemberState actionState(String action) {
        return switch (action) {
            case "APPROVE" -> ApprovalMemberState.APPROVED;
            case "REJECT" -> ApprovalMemberState.REJECTED;
            case "RETURN" -> ApprovalMemberState.RETURNED;
            case "MARK_INELIGIBLE" -> ApprovalMemberState.INELIGIBLE;
            default -> throw new IllegalArgumentException("不支持的审批成员动作：" + action);
        };
    }
}
