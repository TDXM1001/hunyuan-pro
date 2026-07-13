package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalCommandReceiptEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BpmApprovalCommandReceiptDao extends BaseMapper<BpmApprovalCommandReceiptEntity> {

    @Select("SELECT * FROM t_bpm_approval_command_receipt "
            + "WHERE tenant_id = #{tenantId} AND instance_id = #{instanceId} "
            + "AND request_id = #{requestId} FOR UPDATE")
    BpmApprovalCommandReceiptEntity selectForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("instanceId") Long instanceId,
            @Param("requestId") String requestId
    );
}
