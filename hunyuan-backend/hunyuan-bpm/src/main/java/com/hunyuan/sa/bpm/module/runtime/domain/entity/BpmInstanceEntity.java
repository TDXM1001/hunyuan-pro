package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实例投影实体。
 */
@Data
@TableName("t_bpm_instance")
public class BpmInstanceEntity {

    @TableId(type = IdType.AUTO)
    private Long instanceId;

    private String instanceNo;

    private Long definitionId;

    private Long graphDefinitionVersionId;

    private String definitionSource;

    private String engineProcessDefinitionId;

    private String engineProcessInstanceId;

    private String definitionKeySnapshot;

    private Integer definitionVersionSnapshot;

    private Long categoryIdSnapshot;

    private String categoryNameSnapshot;

    private Long startVisibilityPolicyVersionId;

    private String startVisibilityPolicyDigest;

    private String startVisibilityDecisionJson;

    private String title;

    private String summary;

    private Long startEmployeeId;

    private String startEmployeeNameSnapshot;

    private Long startDepartmentIdSnapshot;

    private String startDepartmentNameSnapshot;

    private String businessType;

    private Long businessId;

    private String businessKey;

    private String initialFormDataSnapshotJson;

    private String currentFormDataSnapshotJson;

    private Long formDataVersion;

    private Integer runState;

    private Integer resultState;

    private Integer activeTaskCount;

    private String currentNodeSummaryJson;

    private Long cancelByEmployeeId;

    private String cancelByNameSnapshot;

    private String cancelReason;

    private LocalDateTime startedAt;

    private LocalDateTime lastActionAt;

    private LocalDateTime finishedAt;

    private LocalDateTime cancelledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
