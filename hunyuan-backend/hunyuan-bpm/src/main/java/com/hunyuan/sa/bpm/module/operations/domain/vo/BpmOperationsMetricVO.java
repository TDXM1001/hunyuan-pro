package com.hunyuan.sa.bpm.module.operations.domain.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * BPM 运营治理 SLA、积压和失败趋势指标。
 */
@Data
public class BpmOperationsMetricVO {

    private Long graphDefinitionVersionId;

    private String nodeId;

    private Long organizationId;

    private LocalDate metricDate;

    private String failureCode;

    private long totalCount;

    private long openCount;

    private long slaBreachedCount;

    private long retryableCount;

    private long compensableCount;

    private long averageHandlingMinutes;
}
