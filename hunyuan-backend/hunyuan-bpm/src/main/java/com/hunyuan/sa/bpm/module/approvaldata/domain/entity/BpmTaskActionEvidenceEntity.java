package com.hunyuan.sa.bpm.module.approvaldata.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_bpm_task_action_evidence")
public class BpmTaskActionEvidenceEntity {

    @TableId(type = IdType.AUTO)
    private Long taskActionEvidenceId;

    private Long approvalSubjectSnapshotId;
    private Long taskId;
    private String actionType;
    private Long actorEmployeeId;
    private String actorNameSnapshot;
    private String actionReason;
    private String commentText;
    private String signatureJson;
    private String attachmentsJson;
    private Long beforeWorkingDataVersion;
    private Long afterWorkingDataVersion;
    private String changedFieldsJson;
    private String beforeDataJson;
    private String afterDataJson;
    private String evidenceDigest;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
