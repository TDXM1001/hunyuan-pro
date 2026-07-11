package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程抄送投影实体。
 */
@Data
@TableName("t_bpm_instance_copy")
public class BpmInstanceCopyEntity {

    @TableId(type = IdType.AUTO)
    private Long copyId;

    private Long instanceId;

    private Long definitionId;

    private Long definitionNodeId;

    private String engineProcessInstanceId;

    private String sourceNodeKey;

    private String sourceNodeName;

    private String sourceEventKey;

    private Long targetEmployeeId;

    private String targetNameSnapshot;

    private String copyType;

    private Integer readState;

    private String channelSnapshotJson;

    private String reasonSnapshot;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
