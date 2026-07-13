package com.hunyuan.sa.bpm.module.integration.domain.model;

public record BpmProcessBindingMatch(Long bindingVersionId, Long graphDefinitionVersionId,
                                     String bindingKey, Integer bindingVersion) {
}
