package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 外部回调等待事实。
 */
@Data
@TableName("t_bpm_external_wait")
public class BpmExternalWaitEntity {

    @TableId(type = IdType.AUTO)
    private Long externalWaitId;
    private String correlationKey;
    private String callbackTokenHash;
    private Long instanceId;
    private Long definitionId;
    private Long definitionNodeId;
    private String engineProcessInstanceId;
    private String engineExecutionId;
    private String nodeKey;
    private String connectorKey;
    private Integer connectorVersion;
    private String operationKey;
    private Integer attemptNo;
    private String requestSnapshotJson;
    private String callbackPayloadSnapshotJson;
    private String waitStatus;
    private LocalDateTime timeoutAt;
    private LocalDateTime resumedAt;
    private LocalDateTime cancelledAt;
    private String lastError;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
