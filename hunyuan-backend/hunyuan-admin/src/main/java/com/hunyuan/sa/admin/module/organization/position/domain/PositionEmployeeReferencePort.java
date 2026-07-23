package com.hunyuan.sa.admin.module.organization.position.domain;

/**
 * 岗位对员工引用的只读协作端口。
 */
public interface PositionEmployeeReferencePort {

    /**
     * 统计未逻辑删除员工对岗位的引用数量。
     */
    int countNonDeletedEmployees(Long positionId);
}
