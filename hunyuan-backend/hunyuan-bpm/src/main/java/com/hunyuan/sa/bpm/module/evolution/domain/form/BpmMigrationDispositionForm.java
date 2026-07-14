package com.hunyuan.sa.bpm.module.evolution.domain.form;

import lombok.Data;

@Data
public class BpmMigrationDispositionForm {
    private String action;
    private String reason;
    private String compensationResult;
}
