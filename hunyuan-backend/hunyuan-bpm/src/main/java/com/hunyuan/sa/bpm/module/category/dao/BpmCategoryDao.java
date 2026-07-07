package com.hunyuan.sa.bpm.module.category.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.bpm.module.category.domain.entity.BpmCategoryEntity;
import com.hunyuan.sa.bpm.module.category.domain.form.BpmCategoryQueryForm;
import com.hunyuan.sa.bpm.module.category.domain.vo.BpmCategoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程分类 DAO。
 */
@Mapper
public interface BpmCategoryDao extends BaseMapper<BpmCategoryEntity> {

    /**
     * 分页查询流程分类。
     */
    List<BpmCategoryVO> queryPage(Page page, @Param("queryForm") BpmCategoryQueryForm queryForm);

    /**
     * 按条件查询单条流程分类。
     */
    BpmCategoryEntity selectOne(BpmCategoryEntity entity);
}
