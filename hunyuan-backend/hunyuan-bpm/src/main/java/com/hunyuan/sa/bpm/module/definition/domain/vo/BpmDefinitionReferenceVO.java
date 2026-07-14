package com.hunyuan.sa.bpm.module.definition.domain.vo;

public record BpmDefinitionReferenceVO(
        Long graphDefinitionVersionId, String processKey, String processName,
        Integer definitionVersion, String lifecycleState
) {
}
