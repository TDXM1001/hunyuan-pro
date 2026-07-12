package com.hunyuan.sa.bpm.module.model.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 正式 Graph 作者草稿；不复用旧 SimpleModel 草稿表。
 */
@Data
@TableName("t_bpm_process_draft")
public class BpmProcessDraftEntity {

    @TableId(type = IdType.AUTO)
    private Long draftId;

    private String processKey;

    private String processName;

    private Long categoryId;

    private Integer revision;

    private String graphJson;

    private String layoutJson;

    private String semanticHash;

    private String draftStatus;

    private Long createdByEmployeeId;

    private Long updatedByEmployeeId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
