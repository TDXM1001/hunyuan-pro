package com.hunyuan.sa.bpm.module.operations.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 运营治理异常工单视图。
 */
@Data
public class BpmOperationsCaseVO {

    private Long operationsCaseId;

    private String caseCode;

    private String sourceType;

    private Long sourceId;

    private String eventId;

    private Long instanceId;

    private Long graphDefinitionVersionId;

    private String definitionNodeId;

    private String nodeName;

    private Long organizationId;

    private Long assigneeEmployeeId;

    private String businessType;

    private Long businessId;

    private String businessKey;

    private String caseStatus;

    private String severity;

    private String slaLevel;

    private String failureCode;

    private String failureReason;

    private Boolean retryableFlag;

    private Boolean compensableFlag;

    private Boolean highRiskFlag;

    private Boolean legalHoldFlag;

    private LocalDateTime openedAt;

    private LocalDateTime lastActionAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime retentionUntil;
}
