package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_bpm_sub_process_link")
public class BpmSubProcessLinkEntity {
    @TableId(type = IdType.AUTO)
    private Long subProcessLinkId;
    private String eventKey;
    private Long parentInstanceId;
    private Long parentGraphDefinitionVersionId;
    private String parentNodeId;
    private String parentEngineExecutionId;
    private Long childInstanceId;
    private String childEngineProcessInstanceId;
    private String calledProcessKey;
    private Long calledDefinitionVersionId;
    private String inputSnapshotJson;
    private String outputSnapshotJson;
    private String failurePolicy;
    private String cancelPropagation;
    private String linkStatus;
    private String lastError;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
