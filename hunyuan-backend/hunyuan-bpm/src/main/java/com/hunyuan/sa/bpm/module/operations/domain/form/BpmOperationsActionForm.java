package com.hunyuan.sa.bpm.module.operations.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * BPM 运营治理处置命令。
 */
@Data
public class BpmOperationsActionForm {

    @NotBlank
    private String actionType;

    @NotBlank
    @Size(max = 128)
    private String idempotencyKey;

    @NotBlank
    @Size(max = 512)
    private String reason;
}
