package com.hunyuan.sa.admin.module.organization.department.application;

import com.hunyuan.sa.admin.module.organization.department.domain.Department;
import com.hunyuan.sa.admin.module.organization.department.domain.DepartmentCommand;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationMember;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

import java.util.List;
import java.util.Optional;

public interface OrganizationDepartmentFacade {

    List<Department> list();

    Department get(Long departmentId);

    List<OrganizationMember> listManagerOptions();

    ResponseDTO<Long> create(DepartmentCommand command);

    ResponseDTO<String> update(Long departmentId, DepartmentCommand command);

    ResponseDTO<String> delete(Long departmentId);

    /** 跨模块按编号读取部门，不套用当前登录人的页面数据范围。 */
    Optional<Department> findForCollaboration(Long departmentId);

    /** 跨模块读取完整部门目录，不套用当前登录人的页面数据范围。 */
    List<Department> listForCollaboration();

    /** 返回一组部门编号中不存在的编号。 */
    List<Long> missingIdsForCollaboration(List<Long> departmentIds);

    /** 返回指定部门以及全部下级部门编号。 */
    List<Long> selfAndDescendantIdsForCollaboration(Long departmentId);

    /** 返回从顶级部门到指定部门的名称路径。 */
    String pathForCollaboration(Long departmentId);
}
