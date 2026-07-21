package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.organization.OrganizationModuleAvailability;
import com.hunyuan.sa.admin.module.system.menu.dao.MenuDao;
import com.hunyuan.sa.admin.module.system.menu.domain.vo.MenuVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleMenuServiceTest {

    @Mock
    private MenuDao menuDao;
    @Mock
    private OrganizationModuleAvailability organizationModuleAvailability;

    private RoleMenuService service;

    @BeforeEach
    void setUp() {
        service = new RoleMenuService();
        ReflectionTestUtils.setField(service, "menuDao", menuDao);
        ReflectionTestUtils.setField(service, "organizationModuleAvailability", organizationModuleAvailability);
    }

    @Test
    void administratorLoginIncludesActionCapabilities() {
        MenuVO action = new MenuVO();
        action.setMenuType(3);
        action.setApiPerms("organization.department.create");
        action.setWebPerms("organization.department.create");
        when(menuDao.queryMenuList(false, false, null)).thenReturn(List.of(action));
        when(organizationModuleAvailability.enabled()).thenReturn(true);

        List<MenuVO> result = service.getMenuList(List.of(), true);

        assertThat(result).containsExactly(action);
        verify(menuDao).queryMenuList(false, false, null);
    }
}
