package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeFacade;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeViewType;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDataScopeDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleEmployeeDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleDataScopeEntity;
import com.hunyuan.sa.base.common.util.SmartEnumUtil;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 基于旧角色数据范围表的过渡实现。
 */
@Service
public class AccessDataScopeFacadeAdapter implements AccessDataScopeFacade {

    private static final Integer PERSONAL_VIEW_TYPE = AccessDataScopeViewType.ME.getValue();

    @Resource
    private RoleEmployeeDao roleEmployeeDao;

    @Resource
    private RoleDataScopeDao roleDataScopeDao;

    @Override
    public Integer resolveEmployeeViewType(Long employeeId, Integer dataScopeType) {
        List<Long> roleIds = roleEmployeeDao.selectRoleIdByEmployeeId(employeeId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return PERSONAL_VIEW_TYPE;
        }

        List<RoleDataScopeEntity> dataScopes = roleDataScopeDao.listByRoleIdList(roleIds);
        if (CollectionUtils.isEmpty(dataScopes)) {
            return PERSONAL_VIEW_TYPE;
        }
        return dataScopes.stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(dataScopeType, item.getDataScopeType()))
                .map(RoleDataScopeEntity::getViewType)
                .filter(viewType -> SmartEnumUtil.getEnumByValue(viewType, AccessDataScopeViewType.class) != null)
                .max(Comparator.comparing(this::viewTypeLevel))
                .orElse(PERSONAL_VIEW_TYPE);
    }

    private int viewTypeLevel(Integer viewType) {
        return SmartEnumUtil.getEnumByValue(viewType, AccessDataScopeViewType.class).getLevel();
    }
}
