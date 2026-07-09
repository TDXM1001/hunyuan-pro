package com.hunyuan.sa.bpm.module.integration.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 业务回调记录。
 */
@Data
@TableName("t_bpm_callback_record")
public class BpmCallbackRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long callbackRecordId;

    private String eventId;

    private Long instanceId;

    private String businessType;

    private Long businessId;

    private Integer callbackStatus;

    private String requestPayloadJson;

    private String responsePayloadJson;

    private String failureReason;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    private LocalDateTime compensatedAt;

    private Long compensatedBy;

    private String compensationReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
