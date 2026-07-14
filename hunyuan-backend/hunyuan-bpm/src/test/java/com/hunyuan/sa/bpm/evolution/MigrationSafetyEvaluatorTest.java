package com.hunyuan.sa.bpm.evolution;

import com.hunyuan.sa.bpm.module.evolution.domain.model.MigrationSafetyAssessment;
import com.hunyuan.sa.bpm.module.evolution.domain.model.MigrationSafetyFacts;
import com.hunyuan.sa.bpm.module.evolution.service.MigrationSafetyEvaluator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationSafetyEvaluatorTest {

    private final MigrationSafetyEvaluator evaluator = new MigrationSafetyEvaluator();

    @Test
    void idleSinglePathInstanceWithCompleteMappingsMustBeEligible() {
        MigrationSafetyAssessment result = evaluator.evaluate(new MigrationSafetyFacts(
                true, 0, 0, 0, 0, 0, 0, true, true));

        assertThat(result.eligible()).isTrue();
        assertThat(result.blockers()).isEmpty();
    }

    @Test
    void activeRuntimeFactsAndIncompleteMappingsMustFailClosedWithExplicitReasons() {
        MigrationSafetyAssessment result = evaluator.evaluate(new MigrationSafetyFacts(
                true, 2, 1, 1, 1, 1, 1, false, false));

        assertThat(result.eligible()).isFalse();
        assertThat(result.blockers()).extracting(MigrationSafetyAssessment.Blocker::code)
                .containsExactlyInAnyOrder(
                        "ACTIVE_HUMAN_TASK", "ACTIVE_PARALLEL_PATH", "PENDING_TIMER",
                        "EXTERNAL_WAIT", "ACTIVE_SUB_PROCESS", "IRREVERSIBLE_SIDE_EFFECT",
                        "NODE_MAPPING_INCOMPLETE", "DATA_MAPPING_INVALID");
    }

    @Test
    void finishedOrCancelledInstanceMustNotEnterMigration() {
        MigrationSafetyAssessment result = evaluator.evaluate(new MigrationSafetyFacts(
                false, 0, 0, 0, 0, 0, 0, true, true));

        assertThat(result.blockers()).extracting(MigrationSafetyAssessment.Blocker::code)
                .containsExactly("INSTANCE_NOT_RUNNING");
    }
}
