package com.hunyuan.sa.admin.module.system.datascope.service;

import com.hunyuan.sa.admin.module.access.datascope.api.AccessDepartmentScope;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDepartmentScopeFacade;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeType;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeViewType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 将旧数据范围服务封装为 access 公共部门范围接口。
 */
@Service
public class AccessDepartmentScopeFacadeAdapter implements AccessDepartmentScopeFacade {

    @Resource
    private DataScopeViewService dataScopeViewService;

    @Override
    public AccessDepartmentScope resolveOrganizationDirectoryScope(Long employeeId) {
        AccessDataScopeViewType viewType = dataScopeViewService.getEmployeeDataScopeViewType(
                AccessDataScopeType.ORGANIZATION_DIRECTORY, employeeId);
        List<Long> departmentIds = dataScopeViewService.getCanViewDepartmentId(viewType, employeeId);

        // 旧服务用空列表表示不限制部门，公共接口在此转换为显式语义。
        if (departmentIds.isEmpty()) {
            return AccessDepartmentScope.allDepartments();
        }
        return AccessDepartmentScope.restricted(departmentIds);
    }
}
