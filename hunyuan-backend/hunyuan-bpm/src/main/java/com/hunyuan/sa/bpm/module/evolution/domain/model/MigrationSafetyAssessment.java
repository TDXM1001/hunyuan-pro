package com.hunyuan.sa.bpm.module.evolution.domain.model;

import java.util.List;

public record MigrationSafetyAssessment(boolean eligible, List<Blocker> blockers) {
    public record Blocker(String code, String message) {
    }
}
