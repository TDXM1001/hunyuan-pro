package com.hunyuan.sa.bpm.module.operations.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 运营治理处置审计。
 */
@Data
@TableName("t_bpm_operations_action_log")
public class BpmOperationsActionLogEntity {

    @TableId(type = IdType.AUTO)
    private Long operationsActionLogId;

    private Long operationsCaseId;

    private String actionType;

    private String actionStatus;

    private String idempotencyKey;

    private Long actorEmployeeId;

    private String reason;

    private String beforeSnapshotJson;

    private String afterSnapshotJson;

    private String failureReason;

    private LocalDateTime actionAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
