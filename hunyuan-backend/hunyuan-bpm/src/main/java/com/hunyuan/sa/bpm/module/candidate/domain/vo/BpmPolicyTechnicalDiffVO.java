package com.hunyuan.sa.bpm.module.candidate.domain.vo;

import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;

import java.util.List;

public record BpmPolicyTechnicalDiffVO(
        PolicyReference left, PolicyReference right, List<String> changedPaths
) {
}
