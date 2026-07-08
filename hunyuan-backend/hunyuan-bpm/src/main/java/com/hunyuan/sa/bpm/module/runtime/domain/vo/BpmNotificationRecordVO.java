package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 通知投递记录 VO。
 */
@Data
public class BpmNotificationRecordVO {

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
