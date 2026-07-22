package com.hunyuan.sa.admin.module.system.role.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.admin.module.access.role.api.AccessRole;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleMember;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleMemberQuery;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleMembershipFacade;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleSelection;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.admin.module.organization.department.domain.Department;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleEmployeeDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleEntity;
import com.hunyuan.sa.admin.module.system.role.domain.vo.RoleEmployeeSummaryRow;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于现有角色成员表实现公开查询边界。
 */
@Service
public class AccessRoleMembershipFacadeAdapter implements AccessRoleMembershipFacade {

    @Resource
    private RoleEmployeeDao roleEmployeeDao;

    @Resource
    private RoleDao roleDao;

    @Resource
    private OrganizationDepartmentFacade organizationDepartmentFacade;

    @Override
    public PageResult<AccessRoleMember> queryMembers(AccessRoleMemberQuery query) {
        Page<?> page = SmartPageUtil.convert2PageQuery(query);
        List<AccessRoleMember> employees = roleEmployeeDao.selectRoleEmployeeByName(page, query)
                .stream()
                .filter(Objects::nonNull)
                .map(this::toEmployeeSummary)
                .collect(Collectors.toList());
        fillDepartmentNames(employees);
        return SmartPageUtil.convert2PageResult(page, employees);
    }

    @Override
    public PageResult<AccessRoleMember> queryCandidates(AccessRoleMemberQuery query) {
        Page<?> page = SmartPageUtil.convert2PageQuery(query);
        List<AccessRoleMember> employees = roleEmployeeDao.selectCandidateEmployeeByName(page, query)
                .stream()
                .filter(Objects::nonNull)
                .map(this::toEmployeeSummary)
                .collect(Collectors.toList());
        fillDepartmentNames(employees);
        return SmartPageUtil.convert2PageResult(page, employees);
    }

    @Override
    public List<AccessRoleMember> listMembers(Long roleId) {
        List<AccessRoleMember> employees = roleEmployeeDao.selectEmployeeByRoleId(roleId)
                .stream()
                .filter(Objects::nonNull)
                .map(this::toEmployeeSummary)
                .collect(Collectors.toList());
        fillDepartmentNames(employees);
        return employees;
    }

    @Override
    public List<AccessRoleSelection> listEmployeeRoleSelections(Long employeeId) {
        Set<Long> selectedRoleIds =
                new HashSet<>(roleEmployeeDao.selectRoleIdByEmployeeId(employeeId));
        return roleDao.selectList(null).stream()
                .map(role -> new AccessRoleSelection(
                        role.getRoleId(),
                        role.getRoleName(),
                        role.getRoleCode(),
                        role.getRemark(),
                        selectedRoleIds.contains(role.getRoleId())))
                .toList();
    }

    @Override
    public List<AccessRole> listEmployeeRoles(Long employeeId) {
        return roleEmployeeDao.selectRoleByEmployeeId(employeeId).stream()
                .map(role -> new AccessRole(
                        role.getRoleId(),
                        role.getRoleName(),
                        role.getRoleCode(),
                        role.getRemark()))
                .toList();
    }

    private AccessRoleMember toEmployeeSummary(RoleEmployeeSummaryRow row) {
        return new AccessRoleMember(
                row.getEmployeeId(),
                row.getLoginName(),
                row.getActualName(),
                row.getAvatar(),
                row.getGender(),
                row.getPhone(),
                row.getEmail(),
                row.getDepartmentId(),
                null,
                row.getPositionId(),
                row.getDisabled(),
                row.getCreateTime());
    }

    private void fillDepartmentNames(List<AccessRoleMember> employees) {
        Set<Long> departmentIds = employees.stream()
                .map(AccessRoleMember::departmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (departmentIds.isEmpty()) {
            return;
        }
        var departmentNames = organizationDepartmentFacade.listForCollaboration().stream()
                .filter(department -> departmentIds.contains(department.departmentId()))
                .collect(Collectors.toMap(Department::departmentId, Department::departmentName));
        for (int index = 0; index < employees.size(); index++) {
            AccessRoleMember employee = employees.get(index);
            employees.set(index, employee.withDepartmentName(
                    departmentNames.getOrDefault(employee.departmentId(), "")));
        }
    }
}
