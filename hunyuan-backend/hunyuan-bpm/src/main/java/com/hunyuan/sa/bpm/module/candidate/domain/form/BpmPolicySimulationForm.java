package com.hunyuan.sa.bpm.module.candidate.domain.form;

import com.hunyuan.sa.bpm.module.candidate.domain.visual.BpmPolicyVisualDraft;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class BpmPolicySimulationForm {
    @NotNull
    private BpmPolicyVisualDraft draft;
    @NotNull
    private Long starterEmployeeId;
    private Map<String, Object> routingFacts;
}
