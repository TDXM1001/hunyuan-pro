package com.hunyuan.sa.bpm.module.operations.domain.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 运营治理归档保留评估。
 */
@Data
public class BpmOperationsRetentionEvaluateForm {

    @NotNull
    private Long operationsCaseId;

    private LocalDateTime archiveRequestedAt;
}
