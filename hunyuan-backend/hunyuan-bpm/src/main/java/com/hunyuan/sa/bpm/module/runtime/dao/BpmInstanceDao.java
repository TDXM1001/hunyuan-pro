package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程实例 DAO。
 */
@Mapper
public interface BpmInstanceDao extends BaseMapper<BpmInstanceEntity> {

    /**
     * 分页查询流程实例。
     */
    List<BpmInstanceVO> queryPage(Page page, @Param("queryForm") BpmInstanceQueryForm queryForm);
}
