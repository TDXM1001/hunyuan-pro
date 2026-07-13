package com.hunyuan.sa.bpm.module.approvaldata.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_bpm_approval_subject_snapshot")
public class BpmApprovalSubjectSnapshotEntity {

    @TableId(type = IdType.AUTO)
    private Long approvalSubjectSnapshotId;

    private Long businessContractVersionId;
    private String sourceSystem;
    private String businessType;
    private String businessKey;
    private Long subjectVersion;
    private String title;
    private String summary;
    private String fieldsJson;
    private String lineItemsJson;
    private String attachmentsJson;
    private Long submitterEmployeeId;
    private String submitterNameSnapshot;
    private String snapshotState;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
