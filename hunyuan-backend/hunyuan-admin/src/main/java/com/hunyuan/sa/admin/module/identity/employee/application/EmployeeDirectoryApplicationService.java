package com.hunyuan.sa.admin.module.identity.employee.application;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAuthenticationAccount;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationProfile;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeDirectoryFacade;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeQuery;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeSummary;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeePage;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeRepository;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileFacade;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeDirectoryApplicationService implements EmployeeDirectoryFacade {

    @Resource
    private EmployeeRepository employeeRepository;

    @Resource
    private OrganizationDepartmentFacade organizationDepartmentFacade;

    @Resource
    private PlatformFileFacade platformFileFacade;

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeeAuthenticationAccount> findAuthenticationAccountByLoginName(String loginName) {
        if (StringUtils.isBlank(loginName)) {
            return Optional.empty();
        }
        return employeeRepository.findAuthenticationAccountByLoginName(loginName);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeeAuthenticationAccount> findAuthenticationAccountById(Long employeeId) {
        if (employeeId == null) {
            return Optional.empty();
        }
        return employeeRepository.findAuthenticationAccountById(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeeSummary> findSummaryById(Long employeeId) {
        if (employeeId == null) {
            return Optional.empty();
        }
        return employeeRepository.findSummaryById(employeeId).map(this::enrich);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeeCollaborationProfile> findCollaborationProfileById(Long employeeId) {
        if (employeeId == null) {
            return Optional.empty();
        }
        return employeeRepository.findCollaborationProfileById(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeCollaborationProfile> findCollaborationProfilesByIds(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }
        return employeeRepository.findCollaborationProfilesByIds(employeeIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findNonDeletedEmployeeIdsByDepartmentIds(List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return List.of();
        }
        return employeeRepository.findNonDeletedEmployeeIdsByDepartmentIds(departmentIds);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<EmployeeSummary> query(EmployeeQuery query) {
        List<Long> departmentIds = query.getDepartmentId() == null
                ? List.of()
                : organizationDepartmentFacade.selfAndDescendantIdsForCollaboration(query.getDepartmentId());
        EmployeePage page = employeeRepository.query(query, departmentIds);
        List<EmployeeSummary> employees = page.employees().stream().map(this::enrich).toList();

        PageResult<EmployeeSummary> result = new PageResult<>();
        result.setPageNum(page.pageNum());
        result.setPageSize(page.pageSize());
        result.setTotal(page.total());
        result.setPages(page.pages());
        result.setList(employees);
        result.setEmptyFlag(employees.isEmpty());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean employeeExists(Long employeeId) {
        return employeeId != null && employeeRepository.exists(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countActiveEmployees(Long departmentId) {
        return departmentId == null ? 0 : employeeRepository.countNonDeletedByDepartmentId(departmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSummary> listActiveEmployees() {
        return employeeRepository.findActive();
    }

    private EmployeeSummary enrich(EmployeeSummary employee) {
        EmployeeSummary enriched = employee.withDepartmentName(
                organizationDepartmentFacade.pathForCollaboration(employee.departmentId()));
        if (StringUtils.isBlank(employee.avatar())) {
            return enriched;
        }
        ResponseDTO<String> avatarResponse = platformFileFacade.resolveUrl(employee.avatar());
        return Boolean.TRUE.equals(avatarResponse.getOk())
                ? enriched.withAvatar(avatarResponse.getData())
                : enriched;
    }
}
