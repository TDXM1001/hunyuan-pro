package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程表单数据变更记录。
 */
@Data
public class BpmFormDataChangeVO {
    private Long changeId;
    private Long instanceId;
    private Long taskId;
    private Long definitionNodeId;
    private String nodeKeySnapshot;
    private String changeSource;
    private Long actorEmployeeId;
    private String actorNameSnapshot;
    private Long beforeVersion;
    private Long afterVersion;
    private String changedFieldsJson;
    private String beforeValuesJson;
    private String afterValuesJson;
    private LocalDateTime createTime;
}
