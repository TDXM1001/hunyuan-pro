package com.hunyuan.sa.bpm.module.candidate.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * M2 候选策略的不可变版本目录，供定义发布期冻结引用。
 */
@Data
@TableName("t_bpm_candidate_policy_version")
public class BpmCandidatePolicyVersionEntity {

    @TableId(type = IdType.AUTO)
    private Long candidatePolicyVersionId;

    private String policyKey;

    private Integer policyVersion;

    private String lifecycleState;

    private String policyJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
