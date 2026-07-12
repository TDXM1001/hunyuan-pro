package com.hunyuan.sa.bpm.module.businesscontract.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * M3 业务契约的不可变版本目录，供定义发布期冻结引用。
 */
@Data
@TableName("t_bpm_business_contract_version")
public class BpmBusinessContractVersionEntity {

    @TableId(type = IdType.AUTO)
    private Long businessContractVersionId;

    private String contractKey;

    private Integer contractVersion;

    private String lifecycleState;

    private String contractJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
