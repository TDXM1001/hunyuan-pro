package com.hunyuan.sa.admin.module.bpm.adapter;

import cn.dev33.satoken.stp.StpUtil;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.api.operations.BpmOperationsAccessScope;
import org.springframework.stereotype.Component;

/**
 * 将后台权限与员工所属部门转换为 M7 组织数据范围。
 */
@Component
public class AdminBpmOperationsAccessScope implements BpmOperationsAccessScope {

    private final BpmCurrentActorProvider actorProvider;
    private final BpmOrgIdentityGateway orgIdentityGateway;

    public AdminBpmOperationsAccessScope(
            BpmCurrentActorProvider actorProvider,
            BpmOrgIdentityGateway orgIdentityGateway
    ) {
        this.actorProvider = actorProvider;
        this.orgIdentityGateway = orgIdentityGateway;
    }

    @Override
    public Long requireOrganizationScope(Long requestedOrganizationId) {
        if (StpUtil.hasPermission("bpm:operations:all")) {
            return requestedOrganizationId;
        }
        return currentDepartmentId();
    }

    @Override
    public void checkOrganizationAccess(Long organizationId) {
        if (StpUtil.hasPermission("bpm:operations:all")) {
            return;
        }
        Long departmentId = currentDepartmentId();
        if (organizationId == null || !organizationId.equals(departmentId)) {
            throw new IllegalArgumentException("无权访问该组织的运营治理工单");
        }
    }

    private Long currentDepartmentId() {
        BpmEmployeeSnapshot employee = orgIdentityGateway.requireEmployee(actorProvider.requireCurrentEmployeeId());
        if (employee.departmentId() == null) {
            throw new IllegalArgumentException("当前员工未绑定部门，无法访问运营治理数据");
        }
        return employee.departmentId();
    }
}
