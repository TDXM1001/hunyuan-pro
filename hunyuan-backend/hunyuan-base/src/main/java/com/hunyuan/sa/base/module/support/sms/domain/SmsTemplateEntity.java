package com.hunyuan.sa.base.module.support.sms.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SMS template.
 */
@Data
@TableName("t_sms_template")
public class SmsTemplateEntity {

    @TableId(type = IdType.NONE)
    private String templateCode;

    private String templateName;

    private String templateContent;

    private Boolean disableFlag;

    private String remark;

    private LocalDateTime updateTime;

    private LocalDateTime createTime;
}
