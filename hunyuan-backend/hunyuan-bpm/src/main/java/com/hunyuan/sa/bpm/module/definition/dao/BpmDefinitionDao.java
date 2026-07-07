package com.hunyuan.sa.bpm.module.definition.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.form.BpmDefinitionQueryForm;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionDetailVO;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmStartableDefinitionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程定义 DAO。
 */
@Mapper
public interface BpmDefinitionDao extends BaseMapper<BpmDefinitionEntity> {

    /**
     * 分页查询流程定义。
     */
    List<BpmDefinitionVO> queryPage(Page page, @Param("queryForm") BpmDefinitionQueryForm queryForm);

    /**
     * 查询流程定义详情。
     */
    BpmDefinitionDetailVO queryDetail(@Param("definitionId") Long definitionId);

    /**
     * 查询当前版本定义。
     */
    BpmDefinitionEntity selectCurrentByDefinitionKey(@Param("definitionKey") String definitionKey);

    /**
     * 查询某个定义编码的最大版本号。
     */
    Integer selectMaxVersionByDefinitionKey(@Param("definitionKey") String definitionKey);

    /**
     * 查询员工可发起的流程定义列表。
     */
    List<BpmStartableDefinitionVO> queryStartableList(@Param("employeeId") Long employeeId);
}
