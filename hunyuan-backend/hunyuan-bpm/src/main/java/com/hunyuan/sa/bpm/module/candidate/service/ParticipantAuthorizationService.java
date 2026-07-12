package com.hunyuan.sa.bpm.module.candidate.service;

import com.hunyuan.sa.bpm.module.candidate.domain.model.ActorSnapshot;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberFact;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberState;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalPolicyDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalStageFact;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalStageState;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ParticipantAuthorizationService {

    public boolean authorize(
            ActorSnapshot actor,
            ApprovalStageFact stage,
            ApprovalMemberFact member,
            String action
    ) {
        return actor != null
                && actor.active()
                && stage != null
                && ApprovalStageState.ACTIVE == stage.state()
                && member != null
                && ApprovalMemberState.ACTIVE == member.state()
                && Objects.equals(actor.tenantId(), stage.tenantId())
                && Objects.equals(actor.employeeId(), member.currentEmployeeId())
                && ("APPROVE".equals(action) || "REJECT".equals(action) || "RETURN".equals(action));
    }

    public boolean authorize(
            ActorSnapshot actor,
            ApprovalStageFact stage,
            ApprovalMemberFact member,
            ApprovalPolicyDocument policy,
            String action
    ) {
        return policy != null
                && policy.permitsAction(action)
                && authorize(actor, stage, member, action);
    }
}
