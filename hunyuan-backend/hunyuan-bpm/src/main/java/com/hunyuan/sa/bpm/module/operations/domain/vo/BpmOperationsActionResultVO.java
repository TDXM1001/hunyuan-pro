package com.hunyuan.sa.bpm.module.operations.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BPM 运营治理处置结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BpmOperationsActionResultVO {

    private Long operationsActionLogId;

    private String actionStatus;

    private String message;
}
