package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程任务 DAO。
 */
@Mapper
public interface BpmTaskDao extends BaseMapper<BpmTaskEntity> {

    /**
     * 分页查询流程任务。
     */
    List<BpmTaskVO> queryPage(Page page, @Param("queryForm") BpmTaskQueryForm queryForm);
}
