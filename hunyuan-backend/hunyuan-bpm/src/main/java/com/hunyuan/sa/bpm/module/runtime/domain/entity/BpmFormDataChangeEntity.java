package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程表单数据变更记录。
 */
@Data
@TableName("t_bpm_form_data_change")
public class BpmFormDataChangeEntity {

    @TableId(type = IdType.AUTO)
    private Long changeId;

    private Long instanceId;

    private Long taskId;

    private Long definitionNodeId;

    private String nodeKeySnapshot;

    private String changeSource;

    private Long actorEmployeeId;

    private String actorNameSnapshot;

    private Long beforeVersion;

    private Long afterVersion;

    private String changedFieldsJson;

    private String beforeValuesJson;

    private String afterValuesJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
