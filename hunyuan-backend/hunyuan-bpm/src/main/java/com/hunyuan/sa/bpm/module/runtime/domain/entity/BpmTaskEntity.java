package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程任务投影实体。
 */
@Data
@TableName("t_bpm_task")
public class BpmTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long taskId;

    private Long instanceId;

    private Long definitionId;

    private Long definitionNodeId;

    private String engineTaskId;

    private String engineExecutionId;

    private String engineProcessInstanceId;

    private String taskKey;

    private String taskName;

    private String instanceNo;

    private String instanceTitle;

    private Long startEmployeeId;

    private String startEmployeeNameSnapshot;

    private Long categoryIdSnapshot;

    private String categoryNameSnapshot;

    private Long assigneeEmployeeId;

    private String assigneeNameSnapshot;

    private Long assigneeDepartmentIdSnapshot;

    private String assigneeDepartmentNameSnapshot;

    private String runtimeAssignmentSnapshotJson;

    private Integer taskState;

    private Integer taskResult;

    private LocalDateTime assignedAt;

    private LocalDateTime dueAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime lastActionAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
