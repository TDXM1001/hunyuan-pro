package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程任务动作日志实体。
 */
@Data
@TableName("t_bpm_task_action_log")
public class BpmTaskActionLogEntity {

    @TableId(type = IdType.AUTO)
    private Long actionLogId;

    private Long instanceId;

    private Long taskId;

    private Long definitionId;

    private Long definitionNodeId;

    private String engineTaskId;

    private String actionType;

    private Long actorEmployeeId;

    private String actorNameSnapshot;

    private Long fromAssigneeEmployeeId;

    private Long toAssigneeEmployeeId;

    private String commentText;

    private String actionPayloadJson;

    private LocalDateTime actionAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
