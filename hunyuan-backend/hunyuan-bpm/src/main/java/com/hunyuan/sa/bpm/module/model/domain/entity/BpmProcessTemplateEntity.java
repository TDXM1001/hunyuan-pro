package com.hunyuan.sa.bpm.module.model.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 可复制的 Graph 模板冻结快照。
 */
@Data
@TableName("t_bpm_process_template")
public class BpmProcessTemplateEntity {

    @TableId(type = IdType.AUTO)
    private Long templateId;

    private String templateKey;

    private String templateName;

    private Long categoryId;

    private Long sourceDraftId;

    private String graphJson;

    private String layoutJson;

    private String semanticHash;

    private Boolean enabledFlag;

    private Long createdByEmployeeId;

    private Long updatedByEmployeeId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
