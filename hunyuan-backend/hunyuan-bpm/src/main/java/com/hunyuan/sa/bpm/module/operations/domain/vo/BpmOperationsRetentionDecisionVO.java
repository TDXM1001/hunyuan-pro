package com.hunyuan.sa.bpm.module.operations.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BPM 运营治理归档保留评估结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BpmOperationsRetentionDecisionVO {

    private boolean allowed;

    private String reason;
}
