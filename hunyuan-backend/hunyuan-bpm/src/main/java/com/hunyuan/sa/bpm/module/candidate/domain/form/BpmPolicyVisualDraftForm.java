package com.hunyuan.sa.bpm.module.candidate.domain.form;

import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.ApprovalPolicyVisualDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.BpmPolicyVisualDraft;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.CandidatePolicyVisualDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.StartVisibilityPolicyVisualDocument;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BpmPolicyVisualDraftForm {

    @NotNull
    private PolicyType type;
    @NotBlank
    @Size(max = 128)
    private String policyKey;
    @NotNull
    @Min(1)
    private Integer policyVersion;
    @NotBlank
    @Size(max = 128)
    private String policyName;
    @Size(max = 500)
    private String description;
    @NotNull
    private Long catalogRevision;
    private CandidatePolicyVisualDocument candidate;
    private ApprovalPolicyVisualDocument approval;
    private StartVisibilityPolicyVisualDocument startVisibility;

    public BpmPolicyVisualDraft toVisualDraft() {
        return new BpmPolicyVisualDraft(
                type, policyKey, policyName, description, 2, catalogRevision,
                candidate, approval, startVisibility
        );
    }
}
