package com.hunyuan.sa.bpm.module.model.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmProcessTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Graph 模板快照持久化。
 */
@Mapper
public interface BpmProcessTemplateDao extends BaseMapper<BpmProcessTemplateEntity> {

    @Select("SELECT * FROM t_bpm_process_template WHERE template_key = #{templateKey}")
    BpmProcessTemplateEntity selectByTemplateKey(@Param("templateKey") String templateKey);
}
