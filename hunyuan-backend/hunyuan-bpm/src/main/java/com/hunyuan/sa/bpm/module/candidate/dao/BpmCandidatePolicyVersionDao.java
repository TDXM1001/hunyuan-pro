package com.hunyuan.sa.bpm.module.candidate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmCandidatePolicyVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * M2 候选策略版本目录。
 */
@Mapper
public interface BpmCandidatePolicyVersionDao extends BaseMapper<BpmCandidatePolicyVersionEntity> {
}
