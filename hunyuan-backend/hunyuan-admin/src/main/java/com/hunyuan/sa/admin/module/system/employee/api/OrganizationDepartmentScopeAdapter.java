package com.hunyuan.sa.admin.module.system.employee.api;

import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationDepartmentScopePort;
import com.hunyuan.sa.admin.module.system.datascope.constant.DataScopeTypeEnum;
import com.hunyuan.sa.admin.module.system.datascope.constant.DataScopeViewTypeEnum;
import com.hunyuan.sa.admin.module.system.datascope.service.DataScopeViewService;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrganizationDepartmentScopeAdapter implements OrganizationDepartmentScopePort {

    @Resource
    private DataScopeViewService dataScopeViewService;

    @Override
    public boolean canAccess(Long departmentId) {
        if (departmentId == null) {
            return false;
        }
        List<Long> allowed = allowedDepartmentIds();
        return allowed.isEmpty() || allowed.contains(departmentId);
    }

    @Override
    public boolean canCreateUnder(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return allowedDepartmentIds().isEmpty();
        }
        return canAccess(parentId);
    }

    private List<Long> allowedDepartmentIds() {
        RequestUser requestUser = SmartRequestUtil.getRequestUser();
        if (requestUser == null || requestUser.getUserId() == null) {
            return List.of(0L);
        }
        DataScopeViewTypeEnum viewType = dataScopeViewService.getEmployeeDataScopeViewType(
                DataScopeTypeEnum.ORGANIZATION_DIRECTORY, requestUser.getUserId());
        return dataScopeViewService.getCanViewDepartmentId(viewType, requestUser.getUserId());
    }
}
