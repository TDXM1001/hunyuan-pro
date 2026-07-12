package com.hunyuan.sa.bpm.api.identity;

import java.util.List;

/**
 * BPM 访问当前组织与员工体系的统一入口。
 */
public interface BpmOrgIdentityGateway {

    /**
     * 按员工 id 读取员工快照。
     */
    BpmEmployeeSnapshot requireEmployee(Long employeeId);

    /**
     * 解析部门负责人员工 id。
     */
    Long resolveDepartmentManagerEmployeeId(Long departmentId);

    /**
     * 解析角色下的员工 id 列表。
     */
    List<Long> listEmployeeIdsByRoleId(Long roleId);

    /**
     * 解析岗位下仍有效的员工 id 列表。
     */
    List<Long> listActiveEmployeeIdsByPositionId(Long positionId);
}
