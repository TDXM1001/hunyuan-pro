package com.hunyuan.sa.bpm.module.category.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程分类实体。
 */
@Data
@TableName("t_bpm_category")
public class BpmCategoryEntity {

    @TableId(type = IdType.AUTO)
    private Long categoryId;

    private String categoryCode;

    private String categoryName;

    private String icon;

    private Integer sort;

    private Boolean disabledFlag;

    private String remark;

    private Boolean deletedFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
