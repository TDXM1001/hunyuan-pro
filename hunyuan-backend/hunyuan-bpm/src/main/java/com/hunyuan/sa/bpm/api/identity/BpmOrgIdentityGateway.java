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

    default List<BpmIdentityOptionSnapshot> queryIdentityOptions(
            String kind, String keyword, Long departmentId
    ) {
        return List.of();
    }

    default BpmIdentityOptionSnapshot findIdentityOption(String kind, Long stableId) {
        if (stableId == null) return null;
        return queryIdentityOptions(kind, null, null).stream()
                .filter(option -> stableId.equals(option.stableId())).findFirst().orElse(null);
    }

    /**
     * 解析组织域登记的用户组有效成员。
     */
    default List<Long> listActiveEmployeeIdsByUserGroupId(Long userGroupId) {
        throw new IllegalStateException("组织域未登记 USER_GROUP 能力");
    }

    /**
     * 按最近到最远顺序解析部门负责人链。
     */
    default List<Long> listDepartmentManagerChain(Long departmentId, int maxDepth) {
        throw new IllegalStateException("组织域未登记 DEPARTMENT_MANAGER_CHAIN 能力");
    }

    /**
     * 按最近到最远顺序解析员工汇报主管链。
     */
    default List<Long> listEmployeeReportingManagerChain(Long employeeId, int maxDepth) {
        throw new IllegalStateException("组织域未登记 EMPLOYEE_REPORTING_CHAIN 能力");
    }
}
