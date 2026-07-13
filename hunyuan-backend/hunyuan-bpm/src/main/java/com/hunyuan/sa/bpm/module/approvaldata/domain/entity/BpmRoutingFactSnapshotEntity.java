package com.hunyuan.sa.bpm.module.approvaldata.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_bpm_routing_fact_snapshot")
public class BpmRoutingFactSnapshotEntity {

    @TableId(type = IdType.AUTO)
    private Long routingFactSnapshotId;

    private Long approvalSubjectSnapshotId;
    private Long businessContractVersionId;
    private Long routingFactVersion;
    private String factsJson;
    private String allowedFactKeysJson;
    private String snapshotDigest;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
