package com.hunyuan.sa.bpm.module.evolution.domain.model;

import java.util.List;

public record GraphEvolutionDiff(
        boolean semanticChanged,
        boolean layoutChanged,
        boolean migrationSuggested,
        List<Change> changes
) {
    public record Change(String kind, String elementId, String description) {
    }
}
