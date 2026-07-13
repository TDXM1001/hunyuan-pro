package com.hunyuan.sa.bpm.module.approvaldata.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_bpm_process_working_data")
public class BpmProcessWorkingDataEntity {

    @TableId(type = IdType.AUTO)
    private Long processWorkingDataId;

    private Long approvalSubjectSnapshotId;
    private Long dataVersion;
    private String dataJson;
    private Long actorEmployeeId;
    private String actorNameSnapshot;
    private String changeReason;
    private Long previousDataVersion;
    private String dataDigest;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
