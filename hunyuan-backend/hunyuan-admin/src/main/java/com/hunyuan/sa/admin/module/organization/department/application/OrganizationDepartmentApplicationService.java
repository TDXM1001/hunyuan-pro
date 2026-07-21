package com.hunyuan.sa.admin.module.organization.department.application;

import com.hunyuan.sa.admin.module.organization.department.domain.Department;
import com.hunyuan.sa.admin.module.organization.department.domain.DepartmentCommand;
import com.hunyuan.sa.admin.module.organization.department.domain.DepartmentRepository;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationDepartmentCachePort;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationDirectoryPort;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationDepartmentScopePort;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationMember;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.admin.module.organization.OrganizationBusinessException;
import com.hunyuan.sa.admin.module.organization.OrganizationErrorCode;
import com.hunyuan.sa.admin.module.organization.OrganizationModuleAvailability;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class OrganizationDepartmentApplicationService implements OrganizationDepartmentFacade {

    @Resource
    private DepartmentRepository departmentRepository;

    @Resource
    private OrganizationDirectoryPort organizationDirectoryPort;

    @Resource
    private OrganizationDepartmentCachePort cachePort;

    @Resource
    private OrganizationModuleAvailability moduleAvailability;

    @Resource
    private OrganizationDepartmentScopePort departmentScopePort;

    @Override
    @Transactional(readOnly = true)
    public List<Department> list() {
        moduleAvailability.requireEnabled();
        return departmentRepository.findAll().stream()
                .filter(department -> departmentScopePort.canAccess(department.departmentId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Department get(Long departmentId) {
        moduleAvailability.requireEnabled();
        if (!departmentScopePort.canAccess(departmentId)) {
            throw notFound();
        }
        return departmentRepository.findById(departmentId).orElseThrow(this::notFound);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationMember> listManagerOptions() {
        moduleAvailability.requireEnabled();
        return organizationDirectoryPort.listActiveEmployees().stream()
                .filter(member -> departmentScopePort.canAccess(member.departmentId()))
                .toList();
    }

    @Override
    @Transactional
    public ResponseDTO<Long> create(DepartmentCommand command) {
        moduleAvailability.requireEnabled();
        return createInternal(command, true);
    }

    @Transactional
    @Override
    public ResponseDTO<Long> createForCompatibility(DepartmentCommand command) {
        return createInternal(command, false);
    }

    private ResponseDTO<Long> createInternal(DepartmentCommand command, boolean enforceScope) {
        Department department = normalize(command, null);
        validateParent(department, null, enforceScope);
        validateManager(department.managerId());
        Long departmentId = departmentRepository.insert(department);
        cachePort.clearDepartmentCaches();
        return ResponseDTO.ok(departmentId);
    }

    @Override
    @Transactional
    public ResponseDTO<String> update(Long departmentId, DepartmentCommand command) {
        moduleAvailability.requireEnabled();
        return updateInternal(departmentId, command, true);
    }

    @Transactional
    @Override
    public ResponseDTO<String> updateForCompatibility(Long departmentId, DepartmentCommand command) {
        return updateInternal(departmentId, command, false);
    }

    private ResponseDTO<String> updateInternal(Long departmentId, DepartmentCommand command, boolean enforceScope) {
        if (!departmentRepository.exists(departmentId)) {
            throw notFound();
        }
        if (enforceScope && !departmentScopePort.canAccess(departmentId)) {
            throw notFound();
        }
        Department department = normalize(command, departmentId);
        validateParent(department, departmentId, enforceScope);
        validateManager(department.managerId());
        departmentRepository.update(department);
        cachePort.clearDepartmentCaches();
        return ResponseDTO.ok();
    }

    @Override
    @Transactional
    public ResponseDTO<String> delete(Long departmentId) {
        moduleAvailability.requireEnabled();
        return deleteInternal(departmentId, true);
    }

    @Transactional
    @Override
    public ResponseDTO<String> deleteForCompatibility(Long departmentId) {
        return deleteInternal(departmentId, false);
    }

    private ResponseDTO<String> deleteInternal(Long departmentId, boolean enforceScope) {
        if (!departmentRepository.exists(departmentId)) {
            throw notFound();
        }
        if (enforceScope && !departmentScopePort.canAccess(departmentId)) {
            throw notFound();
        }
        if (departmentRepository.countChildren(departmentId) > 0) {
            throw new OrganizationBusinessException(OrganizationErrorCode.DEPARTMENT_NOT_EMPTY, "请先删除子部门");
        }
        if (organizationDirectoryPort.countActiveEmployees(departmentId) > 0) {
            throw new OrganizationBusinessException(OrganizationErrorCode.DEPARTMENT_NOT_EMPTY, "请先处理部门员工");
        }
        departmentRepository.delete(departmentId);
        cachePort.clearDepartmentCaches();
        return ResponseDTO.ok();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> listForCompatibility() {
        return departmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Department> findForCompatibility(Long departmentId) {
        return departmentRepository.findById(departmentId);
    }

    private Department normalize(DepartmentCommand command, Long departmentId) {
        if (command == null || command.departmentName() == null) {
            throw new OrganizationBusinessException(OrganizationErrorCode.INVALID_DEPARTMENT, "部门名称不能为空");
        }
        String name = command.departmentName().trim();
        if (name.isEmpty() || name.length() > 50) {
            throw new OrganizationBusinessException(OrganizationErrorCode.INVALID_DEPARTMENT, "部门名称长度必须为 1-50 个字符");
        }
        if (command.sort() == null || command.sort() < 0) {
            throw new OrganizationBusinessException(OrganizationErrorCode.INVALID_DEPARTMENT, "排序值不能小于 0");
        }
        return new Department(departmentId, name, command.managerId(), command.parentId(), command.sort(), null, null, null).normalized();
    }

    private void validateParent(Department department, Long updatingId, boolean enforceScope) {
        Long parentId = department.parentId();
        if (parentId == null || Objects.equals(parentId, 0L)) {
            if (enforceScope && !departmentScopePort.canCreateUnder(0L)) {
                throw new OrganizationBusinessException(OrganizationErrorCode.INVALID_DEPARTMENT, "当前数据范围不能创建顶级部门");
            }
            return;
        }
        if (Objects.equals(parentId, updatingId) || !departmentRepository.exists(parentId)) {
            throw new OrganizationBusinessException(OrganizationErrorCode.INVALID_DEPARTMENT, "上级部门不存在或不能选择自身");
        }
        if (enforceScope && !departmentScopePort.canCreateUnder(parentId)) {
            throw new OrganizationBusinessException(OrganizationErrorCode.INVALID_DEPARTMENT, "上级部门不在当前数据范围");
        }
        if (updatingId == null) {
            return;
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0L && visited.add(cursor)) {
            if (Objects.equals(cursor, updatingId)) {
                throw new OrganizationBusinessException(OrganizationErrorCode.INVALID_DEPARTMENT, "不能移动到自己的下级部门");
            }
            cursor = departmentRepository.findById(cursor).map(Department::parentId).orElse(null);
        }
    }

    private void validateManager(Long managerId) {
        if (managerId != null && !organizationDirectoryPort.employeeExists(managerId)) {
            throw new OrganizationBusinessException(OrganizationErrorCode.EMPLOYEE_NOT_FOUND);
        }
    }

    private OrganizationBusinessException notFound() {
        return new OrganizationBusinessException(OrganizationErrorCode.DEPARTMENT_NOT_FOUND);
    }
}
