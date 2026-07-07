package com.hunyuan.sa.bpm.module.model.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmModelEntity;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmModelQueryForm;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmDesignerDetailVO;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmModelVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程模型 DAO。
 */
@Mapper
public interface BpmModelDao extends BaseMapper<BpmModelEntity> {

    /**
     * 分页查询流程模型。
     */
    List<BpmModelVO> queryPage(Page page, @Param("queryForm") BpmModelQueryForm queryForm);

    /**
     * 按条件查询单条流程模型。
     */
    BpmModelEntity selectOne(BpmModelEntity entity);

    /**
     * 获取流程模型详情。
     */
    BpmModelVO queryModelDetail(@Param("modelId") Long modelId);

    /**
     * 获取流程设计器详情。
     */
    BpmDesignerDetailVO queryDesignerDetail(@Param("modelId") Long modelId);
}
