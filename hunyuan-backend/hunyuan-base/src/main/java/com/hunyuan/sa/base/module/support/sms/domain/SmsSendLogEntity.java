package com.hunyuan.sa.base.module.support.sms.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SMS send log.
 */
@Data
@TableName("t_sms_send_log")
public class SmsSendLogEntity {

    @TableId(type = IdType.AUTO)
    private Long smsSendLogId;

    private String provider;

    private String requestId;

    private String phone;

    private String templateCode;

    private String templateContent;

    private String templateParams;

    private String sendContent;

    private String idempotentKey;

    private Integer sendStatus;

    private String failReason;

    private LocalDateTime sendTime;

    private LocalDateTime updateTime;

    private LocalDateTime createTime;
}
