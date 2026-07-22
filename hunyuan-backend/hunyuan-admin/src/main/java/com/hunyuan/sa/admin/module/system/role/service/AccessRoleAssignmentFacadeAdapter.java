package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.role.api.AccessRoleAssignmentFacade;
import com.hunyuan.sa.admin.module.access.role.api.AssignRoleEmployeesCommand;
import com.hunyuan.sa.admin.module.access.role.api.RemoveRoleEmployeesCommand;
import com.hunyuan.sa.admin.module.access.role.api.ReplaceEmployeeRolesCommand;
import com.hunyuan.sa.admin.module.system.role.dao.RoleEmployeeDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleEmployeeEntity;
import com.hunyuan.sa.admin.module.system.role.manager.RoleEmployeeManager;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Transitional implementation over the legacy role membership table.
 */
@Service
public class AccessRoleAssignmentFacadeAdapter implements AccessRoleAssignmentFacade {

    @Resource
    private RoleEmployeeDao roleEmployeeDao;

    @Resource
    private RoleEmployeeManager roleEmployeeManager;

    @Override
    public void assignEmployees(AssignRoleEmployeesCommand command) {
        Set<Long> requestedEmployeeIds = normalize(command.employeeIds());
        if (requestedEmployeeIds.isEmpty()) {
            return;
        }
        Set<Long> assignedEmployeeIds =
                roleEmployeeDao.selectEmployeeIdByRoleIdList(List.of(command.roleId()));
        List<RoleEmployeeEntity> assignments = requestedEmployeeIds.stream()
                .filter(employeeId -> !assignedEmployeeIds.contains(employeeId))
                .map(employeeId -> new RoleEmployeeEntity(command.roleId(), employeeId))
                .toList();
        if (!assignments.isEmpty()) {
            roleEmployeeManager.saveBatch(assignments);
        }
    }

    @Override
    public void removeEmployees(RemoveRoleEmployeesCommand command) {
        Set<Long> employeeIds = normalize(command.employeeIds());
        if (!employeeIds.isEmpty()) {
            roleEmployeeDao.batchDeleteEmployeeRole(command.roleId(), employeeIds);
        }
    }

    @Override
    public void replaceEmployeeRoles(ReplaceEmployeeRolesCommand command) {
        roleEmployeeDao.deleteByEmployeeId(command.employeeId());
        Set<Long> roleIds = normalize(command.roleIds());
        if (!roleIds.isEmpty()) {
            roleEmployeeManager.saveBatch(roleIds.stream()
                    .map(roleId -> new RoleEmployeeEntity(roleId, command.employeeId()))
                    .toList());
        }
    }

    private Set<Long> normalize(Set<Long> ids) {
        return ids == null
                ? Set.of()
                : ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }
}
