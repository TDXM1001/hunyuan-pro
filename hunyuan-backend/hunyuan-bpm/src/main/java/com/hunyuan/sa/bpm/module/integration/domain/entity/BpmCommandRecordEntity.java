package com.hunyuan.sa.bpm.module.integration.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 命令执行记录。
 */
@Data
@TableName("t_bpm_command_record")
public class BpmCommandRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long commandRecordId;

    private String commandKey;

    private String commandType;

    private Long instanceId;

    private String businessType;

    private Long businessId;

    private Integer commandStatus;

    private String requestPayloadJson;

    private String failureReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
