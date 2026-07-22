package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeDefinition;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeManagementFacade;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeManagementFailure;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeManagementResult;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeSetting;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeType;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeViewType;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeViewOption;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessRoleDataScopes;
import com.hunyuan.sa.admin.module.access.datascope.api.ReplaceAccessRoleDataScopesCommand;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDataScopeDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleDataScopeEntity;
import com.hunyuan.sa.admin.module.system.role.manager.RoleDataScopeManager;
import com.hunyuan.sa.base.common.util.SmartEnumUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于旧角色数据范围表实现稳定管理边界。
 */
@Service
public class AccessDataScopeManagementFacadeAdapter implements AccessDataScopeManagementFacade {

    @Resource
    private RoleDao roleDao;

    @Resource
    private RoleDataScopeDao roleDataScopeDao;

    @Resource
    private RoleDataScopeManager roleDataScopeManager;

    @Override
    public List<AccessDataScopeDefinition> listDataScopes() {
        List<AccessDataScopeViewOption> viewOptions = Arrays.stream(AccessDataScopeViewType.values())
                .sorted(Comparator.comparing(AccessDataScopeViewType::getLevel))
                .map(item -> new AccessDataScopeViewOption(
                        item.getValue(),
                        item.getLevel(),
                        item.getDesc()))
                .toList();
        return Arrays.stream(AccessDataScopeType.values())
                .sorted(Comparator.comparing(AccessDataScopeType::getSort))
                .map(item -> new AccessDataScopeDefinition(
                        item.getValue(),
                        item.getName(),
                        item.getDesc(),
                        item.getSort(),
                        viewOptions))
                .toList();
    }

    @Override
    public AccessDataScopeManagementResult<AccessRoleDataScopes> getRoleDataScopes(Long roleId) {
        if (roleDao.selectById(roleId) == null) {
            return AccessDataScopeManagementResult.failure(
                    AccessDataScopeManagementFailure.ROLE_NOT_FOUND,
                    "角色不存在");
        }
        List<AccessDataScopeSetting> dataScopes = roleDataScopeDao.listByRoleId(roleId).stream()
                .map(item -> new AccessDataScopeSetting(
                        item.getDataScopeType(),
                        item.getViewType()))
                .toList();
        return AccessDataScopeManagementResult.success(
                new AccessRoleDataScopes(roleId, dataScopes));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessDataScopeManagementResult<Void> replaceRoleDataScopes(
            ReplaceAccessRoleDataScopesCommand command) {
        if (roleDao.selectById(command.roleId()) == null) {
            return AccessDataScopeManagementResult.failure(
                    AccessDataScopeManagementFailure.ROLE_NOT_FOUND,
                    "角色不存在");
        }
        String validationMessage = validate(command.dataScopes());
        if (validationMessage != null) {
            return AccessDataScopeManagementResult.failure(
                    AccessDataScopeManagementFailure.INVALID_CONFIGURATION,
                    validationMessage);
        }

        List<RoleDataScopeEntity> entities = command.dataScopes().stream()
                .map(item -> toEntity(command.roleId(), item))
                .toList();
        roleDataScopeDao.deleteByRoleId(command.roleId());
        if (!entities.isEmpty()) {
            roleDataScopeManager.saveBatch(entities);
        }
        return AccessDataScopeManagementResult.success(null);
    }

    private String validate(List<AccessDataScopeSetting> dataScopes) {
        Set<Integer> configuredTypes = new HashSet<>();
        for (AccessDataScopeSetting item : dataScopes) {
            if (SmartEnumUtil.getEnumByValue(item.dataScopeType(), AccessDataScopeType.class) == null) {
                return "存在不支持的数据范围类型";
            }
            if (SmartEnumUtil.getEnumByValue(item.viewType(), AccessDataScopeViewType.class) == null) {
                return "存在不支持的可见范围";
            }
            if (!configuredTypes.add(item.dataScopeType())) {
                return "同一数据范围类型不能重复配置";
            }
        }
        return null;
    }

    private RoleDataScopeEntity toEntity(Long roleId, AccessDataScopeSetting item) {
        RoleDataScopeEntity entity = new RoleDataScopeEntity();
        entity.setRoleId(roleId);
        entity.setDataScopeType(item.dataScopeType());
        entity.setViewType(item.viewType());
        return entity;
    }
}
