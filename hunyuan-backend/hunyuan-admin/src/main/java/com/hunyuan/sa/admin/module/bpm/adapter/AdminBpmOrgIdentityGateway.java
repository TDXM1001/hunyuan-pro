package com.hunyuan.sa.admin.module.bpm.adapter;

import com.hunyuan.sa.admin.module.system.department.domain.vo.DepartmentVO;
import com.hunyuan.sa.admin.module.system.department.service.DepartmentService;
import com.hunyuan.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import com.hunyuan.sa.admin.module.system.employee.dao.OrganizationRelationDao;
import com.hunyuan.sa.admin.module.system.employee.service.EmployeeService;
import com.hunyuan.sa.admin.module.system.role.service.RoleEmployeeService;
import com.hunyuan.sa.admin.module.system.role.service.RoleService;
import com.hunyuan.sa.bpm.api.identity.BpmIdentityOptionSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于当前 admin 组织体系实现 BPM 身份解析。
 */
@Component
public class AdminBpmOrgIdentityGateway implements BpmOrgIdentityGateway {

    private final EmployeeService employeeService;

    private final DepartmentService departmentService;

    private final RoleEmployeeService roleEmployeeService;

    private final OrganizationRelationDao organizationRelationDao;
    private final RoleService roleService;

    public AdminBpmOrgIdentityGateway(
            EmployeeService employeeService,
            DepartmentService departmentService,
            RoleEmployeeService roleEmployeeService,
            OrganizationRelationDao organizationRelationDao,
            RoleService roleService
    ) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.roleEmployeeService = roleEmployeeService;
        this.organizationRelationDao = organizationRelationDao;
        this.roleService = roleService;
    }

    @Override
    public BpmEmployeeSnapshot requireEmployee(Long employeeId) {
        EmployeeEntity employeeEntity = employeeService.getById(employeeId);
        if (employeeEntity == null) {
            throw new IllegalArgumentException("员工不存在，employeeId=" + employeeId);
        }
        if (Boolean.TRUE.equals(employeeEntity.getDisabledFlag())) {
            throw new IllegalArgumentException("员工已禁用，employeeId=" + employeeId);
        }
        if (Boolean.TRUE.equals(employeeEntity.getDeletedFlag())) {
            throw new IllegalArgumentException("员工已删除，employeeId=" + employeeId);
        }
        DepartmentVO department = employeeEntity.getDepartmentId() == null
                ? null
                : departmentService.getDepartmentById(employeeEntity.getDepartmentId());
        return new BpmEmployeeSnapshot(
                employeeEntity.getEmployeeId(),
                employeeEntity.getActualName(),
                employeeEntity.getDepartmentId(),
                department == null ? null : department.getDepartmentName(),
                employeeEntity.getPhone(),
                employeeEntity.getEmail()
        );
    }

    @Override
    public Long resolveDepartmentManagerEmployeeId(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        DepartmentVO department = departmentService.getDepartmentById(departmentId);
        return department == null ? null : department.getManagerId();
    }

    @Override
    public List<Long> listEmployeeIdsByRoleId(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        return roleEmployeeService.getAllEmployeeByRoleId(roleId)
                .stream()
                .map(item -> item.getEmployeeId())
                .toList();
    }

    @Override
    public List<Long> listActiveEmployeeIdsByPositionId(Long positionId) {
        return employeeService.listActiveEmployeeIdsByPositionId(positionId);
    }

    @Override
    public List<BpmIdentityOptionSnapshot> queryIdentityOptions(
            String kind, String keyword, Long departmentId
    ) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        if ("EMPLOYEE".equals(kind)) {
            List<com.hunyuan.sa.admin.module.system.employee.domain.vo.EmployeeVO> employees =
                    employeeService.queryAllEmployee(null).getData();
            return employees == null ? List.of() : employees.stream()
                    .filter(employee -> departmentId == null || departmentId.equals(employee.getDepartmentId()))
                    .filter(employee -> normalizedKeyword.isEmpty()
                            || text(employee.getActualName()).contains(normalizedKeyword)
                            || text(employee.getLoginName()).contains(normalizedKeyword))
                    .map(employee -> new BpmIdentityOptionSnapshot(
                            "EMPLOYEE", employee.getEmployeeId(), employee.getActualName(),
                            employee.getDepartmentId(), employee.getDepartmentName(),
                            Boolean.TRUE.equals(employee.getDisabledFlag())
                    )).toList();
        }
        if ("ROLE".equals(kind)) {
            var response = roleService.getAllRole();
            return response.getData() == null ? List.of() : response.getData().stream()
                    .filter(role -> normalizedKeyword.isEmpty()
                            || text(role.getRoleName()).contains(normalizedKeyword)
                            || text(role.getRoleCode()).contains(normalizedKeyword))
                    .map(role -> new BpmIdentityOptionSnapshot(
                            "ROLE", role.getRoleId(), role.getRoleName(), null, null, false
                    )).toList();
        }
        return List.of();
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    @Override
    public List<Long> listActiveEmployeeIdsByUserGroupId(Long userGroupId) {
        if (userGroupId == null || userGroupId <= 0) {
            return Collections.emptyList();
        }
        return organizationRelationDao.listActiveEmployeeIdsByUserGroupId(userGroupId);
    }

    @Override
    public List<Long> listDepartmentManagerChain(Long departmentId, int maxDepth) {
        requireChainArguments(departmentId, maxDepth, "部门主管链");
        List<Long> managers = new ArrayList<>();
        Set<Long> visitedDepartments = new HashSet<>();
        Set<Long> visitedManagers = new HashSet<>();
        Long currentDepartmentId = departmentId;
        for (int depth = 0; depth < maxDepth && currentDepartmentId != null && currentDepartmentId > 0; depth++) {
            if (!visitedDepartments.add(currentDepartmentId)) {
                throw new IllegalStateException("部门主管链存在循环，departmentId=" + currentDepartmentId);
            }
            DepartmentVO department = departmentService.getDepartmentById(currentDepartmentId);
            if (department == null) {
                throw new IllegalStateException("部门主管链引用的部门不存在，departmentId=" + currentDepartmentId);
            }
            Long managerId = department.getManagerId();
            if (managerId != null) {
                requireEmployee(managerId);
                if (!visitedManagers.add(managerId)) {
                    throw new IllegalStateException("部门主管链存在重复主管，employeeId=" + managerId);
                }
                managers.add(managerId);
            }
            currentDepartmentId = department.getParentId();
        }
        if (currentDepartmentId != null && currentDepartmentId > 0) {
            throw new IllegalStateException("部门主管链超过最大深度 " + maxDepth);
        }
        return managers;
    }

    @Override
    public List<Long> listEmployeeReportingManagerChain(Long employeeId, int maxDepth) {
        requireChainArguments(employeeId, maxDepth, "员工汇报链");
        List<Long> managers = new ArrayList<>();
        Set<Long> visitedEmployees = new HashSet<>();
        visitedEmployees.add(employeeId);
        Long currentEmployeeId = employeeId;
        for (int depth = 0; depth < maxDepth; depth++) {
            Long managerId = organizationRelationDao.selectActiveManagerEmployeeId(currentEmployeeId);
            if (managerId == null) {
                return managers;
            }
            if (!visitedEmployees.add(managerId)) {
                throw new IllegalStateException("员工汇报链存在循环，employeeId=" + managerId);
            }
            requireEmployee(managerId);
            managers.add(managerId);
            currentEmployeeId = managerId;
        }
        if (organizationRelationDao.selectActiveManagerEmployeeId(currentEmployeeId) != null) {
            throw new IllegalStateException("员工汇报链超过最大深度 " + maxDepth);
        }
        return managers;
    }

    private void requireChainArguments(Long seedId, int maxDepth, String label) {
        if (seedId == null || seedId <= 0 || maxDepth < 1 || maxDepth > 20) {
            throw new IllegalArgumentException(label + "参数不合法");
        }
    }
}
