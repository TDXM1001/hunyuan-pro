package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeManagementFailure;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeSetting;
import com.hunyuan.sa.admin.module.access.datascope.api.ReplaceAccessRoleDataScopesCommand;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeType;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeViewType;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDataScopeDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleDataScopeEntity;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleEntity;
import com.hunyuan.sa.admin.module.system.role.manager.RoleDataScopeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessDataScopeManagementFacadeAdapterTest {

    @Mock
    private RoleDao roleDao;

    @Mock
    private RoleDataScopeDao roleDataScopeDao;

    @Mock
    private RoleDataScopeManager roleDataScopeManager;

    private AccessDataScopeManagementFacadeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccessDataScopeManagementFacadeAdapter();
        ReflectionTestUtils.setField(adapter, "roleDao", roleDao);
        ReflectionTestUtils.setField(adapter, "roleDataScopeDao", roleDataScopeDao);
        ReflectionTestUtils.setField(adapter, "roleDataScopeManager", roleDataScopeManager);
    }

    @Test
    void listsStableDataScopeCatalogInConfiguredOrder() {
        var definitions = adapter.listDataScopes();

        assertThat(definitions)
                .extracting(item -> item.dataScopeType())
                .containsExactly(
                        AccessDataScopeType.NOTICE.getValue(),
                        AccessDataScopeType.ORGANIZATION_DIRECTORY.getValue());
        assertThat(definitions.get(0).viewOptions())
                .extracting(item -> item.viewType())
                .containsExactly(
                        AccessDataScopeViewType.ME.getValue(),
                        AccessDataScopeViewType.DEPARTMENT.getValue(),
                        AccessDataScopeViewType.DEPARTMENT_AND_SUB.getValue(),
                        AccessDataScopeViewType.ALL.getValue());
    }

    @Test
    void queryRejectsMissingRole() {
        when(roleDao.selectById(9L)).thenReturn(null);

        var result = adapter.getRoleDataScopes(9L);

        assertThat(result.successful()).isFalse();
        assertThat(result.failure()).isEqualTo(AccessDataScopeManagementFailure.ROLE_NOT_FOUND);
        verify(roleDataScopeDao, never()).listByRoleId(9L);
    }

    @Test
    void queryMapsLegacyRelationsToStableSnapshot() {
        when(roleDao.selectById(9L)).thenReturn(new RoleEntity());
        when(roleDataScopeDao.listByRoleId(9L))
                .thenReturn(List.of(dataScope(
                        9L,
                        AccessDataScopeType.NOTICE.getValue(),
                        AccessDataScopeViewType.DEPARTMENT.getValue())));

        var result = adapter.getRoleDataScopes(9L);

        assertThat(result.successful()).isTrue();
        assertThat(result.data().roleId()).isEqualTo(9L);
        assertThat(result.data().dataScopes())
                .containsExactly(new AccessDataScopeSetting(
                        AccessDataScopeType.NOTICE.getValue(),
                        AccessDataScopeViewType.DEPARTMENT.getValue()));
    }

    @Test
    void replaceRejectsMissingRoleWithoutWriting() {
        when(roleDao.selectById(9L)).thenReturn(null);

        var result = adapter.replaceRoleDataScopes(
                new ReplaceAccessRoleDataScopesCommand(9L, List.of()));

        assertThat(result.failure()).isEqualTo(AccessDataScopeManagementFailure.ROLE_NOT_FOUND);
        verify(roleDataScopeDao, never()).deleteByRoleId(9L);
        verify(roleDataScopeManager, never()).saveBatch(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void replaceRejectsUnsupportedOrDuplicateConfiguration() {
        when(roleDao.selectById(9L)).thenReturn(new RoleEntity());

        var unsupported = adapter.replaceRoleDataScopes(
                new ReplaceAccessRoleDataScopesCommand(
                        9L,
                        List.of(new AccessDataScopeSetting(999, 999))));
        var duplicate = adapter.replaceRoleDataScopes(
                new ReplaceAccessRoleDataScopesCommand(
                        9L,
                        List.of(
                                new AccessDataScopeSetting(
                                        AccessDataScopeType.NOTICE.getValue(),
                                        AccessDataScopeViewType.ME.getValue()),
                                new AccessDataScopeSetting(
                                        AccessDataScopeType.NOTICE.getValue(),
                                        AccessDataScopeViewType.ALL.getValue()))));

        assertThat(unsupported.failure())
                .isEqualTo(AccessDataScopeManagementFailure.INVALID_CONFIGURATION);
        assertThat(duplicate.failure())
                .isEqualTo(AccessDataScopeManagementFailure.INVALID_CONFIGURATION);
        verify(roleDataScopeDao, never()).deleteByRoleId(9L);
    }

    @Test
    void replaceDeletesBeforeSavingNewConfiguration() {
        when(roleDao.selectById(9L)).thenReturn(new RoleEntity());
        ArgumentCaptor<List<RoleDataScopeEntity>> entitiesCaptor = ArgumentCaptor.forClass(List.class);

        var result = adapter.replaceRoleDataScopes(
                new ReplaceAccessRoleDataScopesCommand(
                        9L,
                        List.of(new AccessDataScopeSetting(
                                AccessDataScopeType.NOTICE.getValue(),
                                AccessDataScopeViewType.ALL.getValue()))));

        assertThat(result.successful()).isTrue();
        InOrder order = inOrder(roleDataScopeDao, roleDataScopeManager);
        order.verify(roleDataScopeDao).deleteByRoleId(9L);
        order.verify(roleDataScopeManager).saveBatch(entitiesCaptor.capture());
        assertThat(entitiesCaptor.getValue())
                .extracting(
                        RoleDataScopeEntity::getRoleId,
                        RoleDataScopeEntity::getDataScopeType,
                        RoleDataScopeEntity::getViewType)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        9L,
                        AccessDataScopeType.NOTICE.getValue(),
                        AccessDataScopeViewType.ALL.getValue()));
    }

    @Test
    void replaceAllowsClearingAllConfiguration() {
        when(roleDao.selectById(9L)).thenReturn(new RoleEntity());

        var result = adapter.replaceRoleDataScopes(
                new ReplaceAccessRoleDataScopesCommand(9L, List.of()));

        assertThat(result.successful()).isTrue();
        verify(roleDataScopeDao).deleteByRoleId(9L);
        verify(roleDataScopeManager, never()).saveBatch(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void replaceRunsInRollbackTransaction() throws Exception {
        Method method = AccessDataScopeManagementFacadeAdapter.class.getMethod(
                "replaceRoleDataScopes",
                ReplaceAccessRoleDataScopesCommand.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.rollbackFor()).containsExactly(Exception.class);
    }

    private RoleDataScopeEntity dataScope(Long roleId, Integer dataScopeType, Integer viewType) {
        RoleDataScopeEntity entity = new RoleDataScopeEntity();
        entity.setRoleId(roleId);
        entity.setDataScopeType(dataScopeType);
        entity.setViewType(viewType);
        return entity;
    }
}
