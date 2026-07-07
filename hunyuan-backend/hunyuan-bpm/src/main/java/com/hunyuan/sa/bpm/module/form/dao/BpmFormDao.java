package com.hunyuan.sa.bpm.module.form.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.bpm.module.form.domain.entity.BpmFormEntity;
import com.hunyuan.sa.bpm.module.form.domain.form.BpmFormQueryForm;
import com.hunyuan.sa.bpm.module.form.domain.vo.BpmFormVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程表单 DAO。
 */
@Mapper
public interface BpmFormDao extends BaseMapper<BpmFormEntity> {

    /**
     * 分页查询流程表单。
     */
    List<BpmFormVO> queryPage(Page page, @Param("queryForm") BpmFormQueryForm queryForm);

    /**
     * 按条件查询单条流程表单。
     */
    BpmFormEntity selectOne(BpmFormEntity entity);
}
