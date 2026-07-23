package com.hunyuan.sa.admin.module.organization.position.application;

import com.hunyuan.sa.admin.module.organization.position.domain.Position;
import com.hunyuan.sa.admin.module.organization.position.domain.PositionCommand;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

import java.util.List;
import java.util.Optional;

/**
 * 岗位目录公开应用边界。
 */
public interface OrganizationPositionFacade {

    List<Position> list();

    Position get(Long positionId);

    ResponseDTO<Long> create(PositionCommand command);

    ResponseDTO<String> update(Long positionId, PositionCommand command);

    ResponseDTO<String> delete(Long positionId);

    ResponseDTO<String> deleteBatch(List<Long> positionIds);

    /**
     * 跨模块按编号读取岗位，不套用当前登录人的页面数据范围。
     */
    Optional<Position> findForCollaboration(Long positionId);

    /**
     * 跨模块读取有效岗位目录，供员工等模块生成岗位选项。
     */
    List<Position> listForCollaboration();
}
