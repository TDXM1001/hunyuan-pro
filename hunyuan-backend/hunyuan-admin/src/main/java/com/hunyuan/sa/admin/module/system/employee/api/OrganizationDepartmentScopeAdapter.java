package com.hunyuan.sa.admin.module.system.employee.api;

import com.hunyuan.sa.admin.module.access.datascope.api.AccessDepartmentScope;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDepartmentScopeFacade;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationDepartmentScopePort;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrganizationDepartmentScopeAdapter implements OrganizationDepartmentScopePort {

    @Resource
    private AccessDepartmentScopeFacade accessDepartmentScopeFacade;

    @Override
    public boolean canAccess(Long departmentId) {
        if (departmentId == null) {
            return false;
        }
        return currentScope().allows(departmentId);
    }

    @Override
    public boolean canCreateUnder(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return currentScope().unrestricted();
        }
        return canAccess(parentId);
    }

    private AccessDepartmentScope currentScope() {
        RequestUser requestUser = SmartRequestUtil.getRequestUser();
        if (requestUser == null || requestUser.getUserId() == null) {
            // 无请求用户时按无部门权限处理，避免匿名请求获得组织目录访问权。
            return AccessDepartmentScope.restricted(List.of());
        }
        return accessDepartmentScopeFacade.resolveOrganizationDirectoryScope(requestUser.getUserId());
    }
}
