package com.hunyuan.sa.bpm.module.evolution.domain.model;

public record MigrationSafetyFacts(
        boolean running,
        int activeHumanTaskCount,
        int additionalActiveExecutionCount,
        int pendingTimerCount,
        int activeExternalWaitCount,
        int activeSubProcessCount,
        int irreversibleSideEffectCount,
        boolean nodeMappingComplete,
        boolean dataMappingValid
) {
}
