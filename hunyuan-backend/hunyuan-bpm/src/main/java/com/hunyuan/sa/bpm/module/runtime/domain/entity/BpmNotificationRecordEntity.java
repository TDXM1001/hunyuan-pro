package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 通知投递记录。
 */
@Data
@TableName("t_bpm_notification_record")
public class BpmNotificationRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long notificationRecordId;

    private Long instanceId;

    private Long taskId;

    private Long definitionId;

    private Long definitionNodeId;

    private String eventKey;

    private String channel;

    private Long receiverEmployeeId;

    private String receiverSnapshotJson;

    private String templateCode;

    private String title;

    private String contentSnapshot;

    private Integer sendStatus;

    private String requestPayloadJson;

    private String responseSnapshotJson;

    private String failReason;

    private LocalDateTime sentAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
