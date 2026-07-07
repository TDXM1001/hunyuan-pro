package com.hunyuan.sa.bpm.module.form.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程表单实体。
 */
@Data
@TableName("t_bpm_form")
public class BpmFormEntity {

    @TableId(type = IdType.AUTO)
    private Long formId;

    private String formKey;

    private String formName;

    private String schemaJson;

    private String layoutJson;

    private Boolean disabledFlag;

    private String remark;

    private Boolean deletedFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
