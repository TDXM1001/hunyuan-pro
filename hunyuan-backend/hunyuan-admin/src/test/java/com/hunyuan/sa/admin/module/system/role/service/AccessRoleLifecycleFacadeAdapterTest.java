package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.role.api.AccessRoleFailure;
import com.hunyuan.sa.admin.module.access.role.api.CreateAccessRoleCommand;
import com.hunyuan.sa.admin.module.access.role.api.UpdateAccessRoleCommand;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleEmployeeDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleMenuDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleLifecycleFacadeAdapterTest {

    @Mock
    private RoleDao roleDao;

    @Mock
    private RoleMenuDao roleMenuDao;

    @Mock
    private RoleEmployeeDao roleEmployeeDao;

    private AccessRoleLifecycleFacadeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccessRoleLifecycleFacadeAdapter();
        ReflectionTestUtils.setField(adapter, "roleDao", roleDao);
        ReflectionTestUtils.setField(adapter, "roleMenuDao", roleMenuDao);
        ReflectionTestUtils.setField(adapter, "roleEmployeeDao", roleEmployeeDao);
    }

    @Test
    void createRejectsDuplicatedRoleName() {
        when(roleDao.getByRoleName("平台管理员")).thenReturn(role(1L, "平台管理员", "admin"));

        var result = adapter.create(new CreateAccessRoleCommand("平台管理员", "manager", null));

        assertThat(result.successful()).isFalse();
        assertThat(result.failure()).isEqualTo(AccessRoleFailure.ROLE_NAME_DUPLICATED);
        assertThat(result.message()).isEqualTo("角色名称重复");
        verify(roleDao, never()).insert(any(RoleEntity.class));
    }

    @Test
    void createReturnsGeneratedRoleId() {
        when(roleDao.getByRoleName("审计员")).thenReturn(null);
        when(roleDao.getByRoleCode("auditor")).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            RoleEntity entity = invocation.getArgument(0);
            entity.setRoleId(9L);
            return 1;
        }).when(roleDao).insert(any(RoleEntity.class));

        var result = adapter.create(new CreateAccessRoleCommand("审计员", "auditor", "只读审计"));

        assertThat(result.successful()).isTrue();
        assertThat(result.data()).isEqualTo(9L);
    }

    @Test
    void updateRejectsMissingRoleBeforeUniquenessChecks() {
        when(roleDao.selectById(9L)).thenReturn(null);

        var result = adapter.update(new UpdateAccessRoleCommand(9L, "审计员", "auditor", null));

        assertThat(result.failure()).isEqualTo(AccessRoleFailure.ROLE_NOT_FOUND);
        verify(roleDao, never()).getByRoleName(any());
        verify(roleDao, never()).updateById(any(RoleEntity.class));
    }

    @Test
    void updateAllowsCurrentRoleNameAndCode() {
        RoleEntity currentRole = role(9L, "审计员", "auditor");
        when(roleDao.selectById(9L)).thenReturn(currentRole);
        when(roleDao.getByRoleName("审计员")).thenReturn(currentRole);
        when(roleDao.getByRoleCode("auditor")).thenReturn(currentRole);

        var result = adapter.update(new UpdateAccessRoleCommand(9L, "审计员", "auditor", "新备注"));

        assertThat(result.successful()).isTrue();
        verify(roleDao).updateById(any(RoleEntity.class));
    }

    @Test
    void deleteRejectsRoleWithAssignedEmployees() {
        when(roleDao.selectById(9L)).thenReturn(role(9L, "审计员", "auditor"));
        when(roleEmployeeDao.existsByRoleId(9L)).thenReturn(1);

        var result = adapter.delete(9L);

        assertThat(result.failure()).isEqualTo(AccessRoleFailure.ROLE_HAS_EMPLOYEES);
        assertThat(result.message()).isEqualTo("该角色下存在员工，无法删除");
        verify(roleDao, never()).deleteById(9L);
        verify(roleMenuDao, never()).deleteByRoleId(9L);
    }

    @Test
    void deleteCleansRoleAndRelationsInLegacyOrder() {
        when(roleDao.selectById(9L)).thenReturn(role(9L, "审计员", "auditor"));
        when(roleEmployeeDao.existsByRoleId(9L)).thenReturn(null);

        var result = adapter.delete(9L);

        assertThat(result.successful()).isTrue();
        InOrder order = inOrder(roleDao, roleMenuDao, roleEmployeeDao);
        order.verify(roleDao).deleteById(9L);
        order.verify(roleMenuDao).deleteByRoleId(9L);
        order.verify(roleEmployeeDao).deleteByRoleId(9L);
    }

    @Test
    void getReturnsStablePublicRoleModel() {
        when(roleDao.selectById(9L)).thenReturn(role(9L, "审计员", "auditor"));

        var result = adapter.get(9L);

        assertThat(result.successful()).isTrue();
        assertThat(result.data().roleId()).isEqualTo(9L);
        assertThat(result.data().roleName()).isEqualTo("审计员");
        assertThat(result.data().roleCode()).isEqualTo("auditor");
    }

    private RoleEntity role(Long roleId, String roleName, String roleCode) {
        RoleEntity role = new RoleEntity();
        role.setRoleId(roleId);
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        return role;
    }
}
