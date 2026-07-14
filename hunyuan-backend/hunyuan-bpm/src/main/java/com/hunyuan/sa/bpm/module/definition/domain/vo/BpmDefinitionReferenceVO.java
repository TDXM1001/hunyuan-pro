package com.hunyuan.sa.bpm.module.definition.domain.vo;

public record BpmDefinitionReferenceVO(
        Long graphDefinitionVersionId, Long draftId, String referenceSource,
        String processKey, String processName, Integer definitionVersion, String lifecycleState
) {
}
