package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 时间事件与 SLA 运行事实。
 */
@Data
@TableName("t_bpm_time_event")
public class BpmTimeEventEntity {

    @TableId(type = IdType.AUTO)
    private Long timeEventId;
    private String eventKey;
    private String idempotencyKey;
    private Long instanceId;
    private Long taskId;
    private Long definitionId;
    private Long graphDefinitionVersionId;
    private Long definitionNodeId;
    private String nodeKey;
    private String engineProcessInstanceId;
    private String engineExecutionId;
    private String engineTaskId;
    private String engineJobId;
    private String eventKind;
    private String policySnapshotJson;
    private LocalDateTime scheduledAt;
    private LocalDateTime triggeredAt;
    private LocalDateTime completedAt;
    private String eventStatus;
    private Integer triggerCount;
    private String lastError;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
