package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.system.datascope.constant.DataScopeTypeEnum;
import com.hunyuan.sa.admin.module.system.datascope.constant.DataScopeViewTypeEnum;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDataScopeDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleEmployeeDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleDataScopeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessDataScopeFacadeAdapterTest {

    @Mock
    private RoleEmployeeDao roleEmployeeDao;

    @Mock
    private RoleDataScopeDao roleDataScopeDao;

    private AccessDataScopeFacadeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccessDataScopeFacadeAdapter();
        ReflectionTestUtils.setField(adapter, "roleEmployeeDao", roleEmployeeDao);
        ReflectionTestUtils.setField(adapter, "roleDataScopeDao", roleDataScopeDao);
    }

    @Test
    void missingRolesFallBackToPersonalView() {
        when(roleEmployeeDao.selectRoleIdByEmployeeId(7L)).thenReturn(List.of());

        assertThat(adapter.resolveEmployeeViewType(7L, DataScopeTypeEnum.NOTICE.getValue()))
                .isEqualTo(DataScopeViewTypeEnum.ME.getValue());
        verify(roleDataScopeDao, never()).listByRoleIdList(List.of());
    }

    @Test
    void resolvesStrongestConfiguredViewForRequestedDataScope() {
        when(roleEmployeeDao.selectRoleIdByEmployeeId(7L)).thenReturn(List.of(3L, 4L));
        when(roleDataScopeDao.listByRoleIdList(List.of(3L, 4L)))
                .thenReturn(List.of(
                        dataScope(3L, DataScopeTypeEnum.NOTICE.getValue(),
                                DataScopeViewTypeEnum.DEPARTMENT.getValue()),
                        dataScope(4L, DataScopeTypeEnum.NOTICE.getValue(),
                                DataScopeViewTypeEnum.DEPARTMENT_AND_SUB.getValue()),
                        dataScope(4L, DataScopeTypeEnum.ORGANIZATION_DIRECTORY.getValue(),
                                DataScopeViewTypeEnum.ALL.getValue())));

        assertThat(adapter.resolveEmployeeViewType(7L, DataScopeTypeEnum.NOTICE.getValue()))
                .isEqualTo(DataScopeViewTypeEnum.DEPARTMENT_AND_SUB.getValue());
    }

    @Test
    void missingDataScopeConfigurationFallsBackToPersonalView() {
        when(roleEmployeeDao.selectRoleIdByEmployeeId(7L)).thenReturn(List.of(3L));
        when(roleDataScopeDao.listByRoleIdList(List.of(3L))).thenReturn(List.of());

        assertThat(adapter.resolveEmployeeViewType(7L, DataScopeTypeEnum.NOTICE.getValue()))
                .isEqualTo(DataScopeViewTypeEnum.ME.getValue());
    }

    private RoleDataScopeEntity dataScope(Long roleId, Integer dataScopeType, Integer viewType) {
        RoleDataScopeEntity entity = new RoleDataScopeEntity();
        entity.setRoleId(roleId);
        entity.setDataScopeType(dataScopeType);
        entity.setViewType(viewType);
        return entity;
    }
}
