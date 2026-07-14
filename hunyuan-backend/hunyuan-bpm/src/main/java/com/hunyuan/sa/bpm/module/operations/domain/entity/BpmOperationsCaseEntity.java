package com.hunyuan.sa.bpm.module.operations.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 运营治理统一异常工单。
 */
@Data
@TableName("t_bpm_operations_case")
public class BpmOperationsCaseEntity {

    @TableId(type = IdType.AUTO)
    private Long operationsCaseId;

    private String caseCode;

    private String sourceType;

    private Long sourceId;

    private String eventId;

    private Long instanceId;

    private Long definitionId;

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

    private String idempotencyKey;

    private Boolean retryableFlag;

    private Boolean compensableFlag;

    private Boolean highRiskFlag;

    private Boolean legalHoldFlag;

    private Integer businessEvidenceRefCount;

    private Integer migrationSourceRefCount;

    private String beforeSnapshotJson;

    private String afterSnapshotJson;

    private LocalDateTime openedAt;

    private LocalDateTime lastActionAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime retentionUntil;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
