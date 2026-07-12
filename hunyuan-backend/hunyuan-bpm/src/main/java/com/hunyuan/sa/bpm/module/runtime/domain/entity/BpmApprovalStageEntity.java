package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批节点一次运行生成的冻结阶段事实。
 */
@Data
@TableName("t_bpm_approval_stage")
public class BpmApprovalStageEntity {

    @TableId(type = IdType.AUTO)
    private Long approvalStageId;

    private Long instanceId;

    private Long tenantId;

    private Long definitionVersionId;

    private String authoredNodeId;

    private Integer generation;

    private String stageInvocationId;

    private String engineProcessInstanceId;

    private String engineExecutionId;

    private String stageState;

    private String terminalReason;

    private String engineEffectState;

    private LocalDateTime engineEffectClaimedAt;

    private LocalDateTime engineEffectCompletedAt;

    private String engineEffectError;

    private String completionMode;

    private Integer ratioPercent;

    private String rejectionRule;

    private Integer effectiveMemberCount;

    private Integer requiredApprovalCount;

    private Long candidatePolicyVersionId;

    private String candidatePolicyDigest;

    private Long approvalPolicyVersionId;

    private String approvalPolicyDigest;

    private String approvalPolicySnapshotJson;

    private String candidateSnapshotJson;

    private String candidateSnapshotDigest;

    private String diagnosticsJson;

    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    @Version
    private Integer revision;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
