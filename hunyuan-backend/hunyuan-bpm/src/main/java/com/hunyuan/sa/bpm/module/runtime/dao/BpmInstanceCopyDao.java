package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceCopyEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCopyQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceCopyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程抄送 DAO。
 */
@Mapper
public interface BpmInstanceCopyDao extends BaseMapper<BpmInstanceCopyEntity> {

    List<BpmInstanceCopyVO> queryMyCopyPage(Page page, @Param("queryForm") BpmInstanceCopyQueryForm queryForm);
}
