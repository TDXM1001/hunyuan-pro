package com.hunyuan.sa.bpm.module.evolution.service;

import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.model.MigrationSafetyAssessment;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import java.util.Map;

public interface BpmMigrationRuntimeGateway {
    MigrationSafetyAssessment assess(BpmInstanceEntity instance, GraphDefinitionVersionEntity target,
                                     Map<String, String> authoredNodeMappings, String dataMappingJson);
    MigrationRuntimeEvidence migrate(BpmInstanceEntity instance, GraphDefinitionVersionEntity target,
                                     Map<String, String> authoredNodeMappings, String dataMappingJson);
    MigrationRuntimeEvidence inspectSourceVariables(BpmInstanceEntity instance, String dataMappingJson);
    MigrationRuntimeEvidence inspectTargetVariables(BpmInstanceEntity instance, String dataMappingJson);
    String currentEngineDefinitionId(BpmInstanceEntity instance);

    record MigrationRuntimeEvidence(Map<String, String> mappedVariableNames,
                                    String sourceVariablesDigest,
                                    String targetVariablesDigest) {
        public static MigrationRuntimeEvidence empty() {
            return new MigrationRuntimeEvidence(Map.of(), null, null);
        }
    }
}
