package com.hunyuan.sa.bpm.module.evolution.domain.vo;

import com.hunyuan.sa.bpm.module.evolution.domain.model.GraphEvolutionDiff;
import lombok.Data;
import java.util.List;

@Data
public class GraphEvolutionDiffVO {
    private Long sourceVersionId;
    private Long targetVersionId;
    private boolean semanticChanged;
    private boolean layoutChanged;
    private boolean migrationSuggested;
    private List<GraphEvolutionDiff.Change> changes;
}
