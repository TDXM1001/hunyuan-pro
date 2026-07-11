package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 不可变的流程路由决定事实。
 */
@Data
@TableName("t_bpm_route_decision")
public class BpmRouteDecisionEntity {

    @TableId(type = IdType.AUTO)
    private Long routeDecisionId;

    private Long instanceId;
    private Long definitionId;
    private Long definitionNodeId;
    private String engineProcessInstanceId;
    private String routeNodeKey;
    private Long inputFormDataVersion;
    private String matchedBranchKeysJson;
    private Boolean defaultBranchUsed;
    private String evaluationStatus;
    private String reasonSnapshotJson;
    private LocalDateTime evaluatedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
