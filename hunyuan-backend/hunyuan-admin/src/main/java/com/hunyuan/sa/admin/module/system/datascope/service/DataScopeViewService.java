package com.hunyuan.sa.admin.module.system.datascope.service;

import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeFacade;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeType;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeViewType;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationProfile;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationDirectory;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.base.common.util.SmartEnumUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据范围
 *
 * @Author 1024创新实验室: 罗伊
 * @Date 2020/11/28  20:59:17
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
public class DataScopeViewService {

    @Resource
    private AccessDataScopeFacade accessDataScopeFacade;

    @Resource
    private EmployeeCollaborationDirectory employeeCollaborationDirectory;

    @Resource
    // 延迟解析组织目录，避免数据范围适配器在启动阶段形成循环依赖。
    @Lazy
    private OrganizationDepartmentFacade organizationDepartmentFacade;

    /**
     * 获取某人可以查看的所有人员数据
     */
    public List<Long> getCanViewEmployeeId(AccessDataScopeViewType viewType, Long employeeId) {
        if (AccessDataScopeViewType.ME == viewType) {
            return this.getMeEmployeeIdList(employeeId);
        }
        if (AccessDataScopeViewType.DEPARTMENT == viewType) {
            return this.getDepartmentEmployeeIdList(employeeId);
        }
        if (AccessDataScopeViewType.DEPARTMENT_AND_SUB == viewType) {
            return this.getDepartmentAndSubEmployeeIdList(employeeId);
        }
        // 可以查看所有员工数据
        return Lists.newArrayList();
    }

    /**
     * 获取某人可以查看的所有部门数据
     */
    public List<Long> getCanViewDepartmentId(AccessDataScopeViewType viewType, Long employeeId) {
        if (AccessDataScopeViewType.ME == viewType) {
            // 数据可见范围类型为本人时 不可以查看任何部门数据
            return Lists.newArrayList(0L);
        }
        if (AccessDataScopeViewType.DEPARTMENT == viewType) {
            return this.getMeDepartmentIdList(employeeId);
        }
        if (AccessDataScopeViewType.DEPARTMENT_AND_SUB == viewType) {
            return this.getDepartmentAndSubIdList(employeeId);
        }
        // 可以查看所有部门数据
        return Lists.newArrayList();
    }

    public List<Long> getMeDepartmentIdList(Long employeeId) {
        EmployeeCollaborationProfile employee = getRequiredEmployee(employeeId);
        return Lists.newArrayList(employee.departmentId());
    }

    public List<Long> getDepartmentAndSubIdList(Long employeeId) {
        EmployeeCollaborationProfile employee = getRequiredEmployee(employeeId);
        return organizationDepartmentFacade.selfAndDescendantIdsForCollaboration(employee.departmentId());
    }

    /**
     * 根据员工id 获取各数据范围最大的可见范围 map<dataScopeType,viewType></>
     */
    public AccessDataScopeViewType getEmployeeDataScopeViewType(
            AccessDataScopeType dataScopeType, Long employeeId) {
        EmployeeCollaborationProfile employee = employeeCollaborationDirectory.findCollaborationProfileById(employeeId)
                .orElse(null);
        if (employee == null || employee.employeeId() == null) {
            return AccessDataScopeViewType.ME;
        }

        // 如果是超级管理员 则可查看全部
        if (Boolean.TRUE.equals(employee.administrator())) {
            return AccessDataScopeViewType.ALL;
        }

        Integer viewType = accessDataScopeFacade.resolveEmployeeViewType(
                employeeId, dataScopeType.getValue());
        AccessDataScopeViewType resolvedViewType =
                SmartEnumUtil.getEnumByValue(viewType, AccessDataScopeViewType.class);
        return resolvedViewType == null ? AccessDataScopeViewType.ME : resolvedViewType;
    }

    /**
     * 获取本人相关 可查看员工id
     */
    private List<Long> getMeEmployeeIdList(Long employeeId) {
        return Lists.newArrayList(employeeId);
    }

    /**
     * 获取本部门相关 可查看员工id
     */
    private List<Long> getDepartmentEmployeeIdList(Long employeeId) {
        EmployeeCollaborationProfile employee = getRequiredEmployee(employeeId);
        return employeeCollaborationDirectory.findNonDeletedEmployeeIdsByDepartmentIds(
                Lists.newArrayList(employee.departmentId()));
    }

    /**
     * 获取本部门及下属子部门相关 可查看员工id
     */
    private List<Long> getDepartmentAndSubEmployeeIdList(Long employeeId) {
        List<Long> allDepartmentIds = getDepartmentAndSubIdList(employeeId);
        return employeeCollaborationDirectory.findNonDeletedEmployeeIdsByDepartmentIds(allDepartmentIds);
    }

    private EmployeeCollaborationProfile getRequiredEmployee(Long employeeId) {
        return employeeCollaborationDirectory.findCollaborationProfileById(employeeId)
                .orElseThrow(() -> new IllegalStateException("Employee does not exist: " + employeeId));
    }
}
