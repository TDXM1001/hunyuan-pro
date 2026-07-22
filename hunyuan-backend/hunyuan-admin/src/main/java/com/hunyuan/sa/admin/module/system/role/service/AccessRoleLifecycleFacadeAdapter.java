package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.role.api.AccessRole;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleFailure;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleLifecycleFacade;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleResult;
import com.hunyuan.sa.admin.module.access.role.api.CreateAccessRoleCommand;
import com.hunyuan.sa.admin.module.access.role.api.UpdateAccessRoleCommand;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleEmployeeDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleMenuDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 基于旧角色数据表实现角色生命周期公开边界。
 */
@Service
public class AccessRoleLifecycleFacadeAdapter implements AccessRoleLifecycleFacade {

    @Resource
    private RoleDao roleDao;

    @Resource
    private RoleMenuDao roleMenuDao;

    @Resource
    private RoleEmployeeDao roleEmployeeDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessRoleResult<Long> create(CreateAccessRoleCommand command) {
        RoleEntity sameNameRole = roleDao.getByRoleName(command.roleName());
        if (sameNameRole != null) {
            return AccessRoleResult.failure(AccessRoleFailure.ROLE_NAME_DUPLICATED, "角色名称重复");
        }

        RoleEntity sameCodeRole = roleDao.getByRoleCode(command.roleCode());
        if (sameCodeRole != null) {
            return AccessRoleResult.failure(
                    AccessRoleFailure.ROLE_CODE_DUPLICATED,
                    "角色编码重复，重复的角色为：" + sameCodeRole.getRoleName());
        }

        RoleEntity roleEntity = toEntity(
                null,
                command.roleName(),
                command.roleCode(),
                command.remark());
        roleDao.insert(roleEntity);
        return AccessRoleResult.success(roleEntity.getRoleId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessRoleResult<Void> update(UpdateAccessRoleCommand command) {
        if (roleDao.selectById(command.roleId()) == null) {
            return AccessRoleResult.failure(AccessRoleFailure.ROLE_NOT_FOUND, null);
        }

        RoleEntity sameNameRole = roleDao.getByRoleName(command.roleName());
        if (sameNameRole != null && !sameNameRole.getRoleId().equals(command.roleId())) {
            return AccessRoleResult.failure(AccessRoleFailure.ROLE_NAME_DUPLICATED, "角色名称重复");
        }

        RoleEntity sameCodeRole = roleDao.getByRoleCode(command.roleCode());
        if (sameCodeRole != null && !sameCodeRole.getRoleId().equals(command.roleId())) {
            return AccessRoleResult.failure(
                    AccessRoleFailure.ROLE_CODE_DUPLICATED,
                    "角色编码重复，重复的角色为：" + sameCodeRole.getRoleName());
        }

        roleDao.updateById(toEntity(
                command.roleId(),
                command.roleName(),
                command.roleCode(),
                command.remark()));
        return AccessRoleResult.success(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessRoleResult<Void> delete(Long roleId) {
        if (roleDao.selectById(roleId) == null) {
            return AccessRoleResult.failure(AccessRoleFailure.ROLE_NOT_FOUND, null);
        }
        if (roleEmployeeDao.existsByRoleId(roleId) != null) {
            return AccessRoleResult.failure(
                    AccessRoleFailure.ROLE_HAS_EMPLOYEES,
                    "该角色下存在员工，无法删除");
        }

        roleDao.deleteById(roleId);
        roleMenuDao.deleteByRoleId(roleId);
        roleEmployeeDao.deleteByRoleId(roleId);
        return AccessRoleResult.success(null);
    }

    @Override
    public AccessRoleResult<AccessRole> get(Long roleId) {
        RoleEntity roleEntity = roleDao.selectById(roleId);
        if (roleEntity == null) {
            return AccessRoleResult.failure(AccessRoleFailure.ROLE_NOT_FOUND, null);
        }
        return AccessRoleResult.success(toAccessRole(roleEntity));
    }

    @Override
    public List<AccessRole> list() {
        return roleDao.selectList(null).stream()
                .map(this::toAccessRole)
                .toList();
    }

    private RoleEntity toEntity(Long roleId, String roleName, String roleCode, String remark) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRoleId(roleId);
        roleEntity.setRoleName(roleName);
        roleEntity.setRoleCode(roleCode);
        roleEntity.setRemark(remark);
        return roleEntity;
    }

    private AccessRole toAccessRole(RoleEntity roleEntity) {
        return new AccessRole(
                roleEntity.getRoleId(),
                roleEntity.getRoleName(),
                roleEntity.getRoleCode(),
                roleEntity.getRemark());
    }
}
