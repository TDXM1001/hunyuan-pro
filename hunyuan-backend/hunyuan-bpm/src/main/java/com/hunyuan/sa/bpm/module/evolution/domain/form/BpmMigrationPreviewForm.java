package com.hunyuan.sa.bpm.module.evolution.domain.form;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class BpmMigrationPreviewForm {
    private Long sourceVersionId;
    private Long targetVersionId;
    private List<Long> instanceIds;
    private Map<String, String> nodeMappings;
    private String dataMappingJson;
    private String idempotencyKey;
    private String reason;
}
