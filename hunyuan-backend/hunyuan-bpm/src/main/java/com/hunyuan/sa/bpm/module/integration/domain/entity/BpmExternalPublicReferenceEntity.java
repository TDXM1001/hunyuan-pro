package com.hunyuan.sa.bpm.module.integration.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_bpm_external_public_reference")
public class BpmExternalPublicReferenceEntity {
    @TableId(type = IdType.AUTO)
    private Long publicReferenceId;
    private String publicId;
    private String sourceSystemCode;
    private String objectType;
    private Long internalId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
