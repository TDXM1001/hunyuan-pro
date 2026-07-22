package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.role.api.AssignRoleEmployeesCommand;
import com.hunyuan.sa.admin.module.access.role.api.RemoveRoleEmployeesCommand;
import com.hunyuan.sa.admin.module.access.role.api.ReplaceEmployeeRolesCommand;
import com.hunyuan.sa.admin.module.system.role.dao.RoleEmployeeDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleEmployeeEntity;
import com.hunyuan.sa.admin.module.system.role.manager.RoleEmployeeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleAssignmentFacadeAdapterTest {

    @Mock
    private RoleEmployeeDao roleEmployeeDao;

    @Mock
    private RoleEmployeeManager roleEmployeeManager;

    private AccessRoleAssignmentFacadeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccessRoleAssignmentFacadeAdapter();
        ReflectionTestUtils.setField(adapter, "roleEmployeeDao", roleEmployeeDao);
        ReflectionTestUtils.setField(adapter, "roleEmployeeManager", roleEmployeeManager);
    }

    @Test
    void assignEmployeesOnlyPersistsMissingMemberships() {
        when(roleEmployeeDao.selectEmployeeIdByRoleIdList(List.of(3L)))
                .thenReturn(Set.of(7L));

        adapter.assignEmployees(new AssignRoleEmployeesCommand(3L, Set.of(7L, 8L)));

        ArgumentCaptor<List<RoleEmployeeEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(roleEmployeeManager).saveBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(RoleEmployeeEntity::getRoleId, RoleEmployeeEntity::getEmployeeId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(3L, 8L));
    }

    @Test
    void removeEmployeesIgnoresEmptyCommands() {
        adapter.removeEmployees(new RemoveRoleEmployeesCommand(3L, Set.of()));

        verify(roleEmployeeDao, never()).batchDeleteEmployeeRole(3L, Set.of());
    }

    @Test
    void replaceEmployeeRolesDeletesOldMembershipsBeforeSavingNewOnes() {
        adapter.replaceEmployeeRoles(new ReplaceEmployeeRolesCommand(7L, Set.of(3L, 4L)));

        verify(roleEmployeeDao).deleteByEmployeeId(7L);
        ArgumentCaptor<List<RoleEmployeeEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(roleEmployeeManager).saveBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(RoleEmployeeEntity::getEmployeeId)
                .containsOnly(7L);
        assertThat(captor.getValue())
                .extracting(RoleEmployeeEntity::getRoleId)
                .containsExactlyInAnyOrder(3L, 4L);
    }
}
