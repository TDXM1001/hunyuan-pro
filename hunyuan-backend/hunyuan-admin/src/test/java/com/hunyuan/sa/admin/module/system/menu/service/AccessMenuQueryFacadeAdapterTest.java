package com.hunyuan.sa.admin.module.system.menu.service;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessMenuQueryFacadeAdapterTest {

    @Mock
    private MenuDao menuDao;

    private AccessMenuQueryFacadeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccessMenuQueryFacadeAdapter();
        ReflectionTestUtils.setField(adapter, "menuDao", menuDao);
    }

    @Test
    void listEnabledMenusPreservesMenuFields() {
        MenuVO menu = menu(8L, "财务", "invoice.read");
        when(menuDao.queryMenuList(false, false, null)).thenReturn(List.of(menu));

        var result = adapter.listEnabledMenus();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.menuId()).isEqualTo(8L);
            assertThat(item.menuName()).isEqualTo("财务");
            assertThat(item.apiPerms()).isEqualTo("invoice.read");
        });
    }

    @Test
    void listAuthorizedMenusByRoleIdsUsesOwnerDaoQuery() {
        MenuVO menu = menu(9L, "角色管理", "access.role.read");
        when(menuDao.queryAuthorizedMenuListByRoleIds(List.of(2L, 3L)))
                .thenReturn(List.of(menu));

        var result = adapter.listAuthorizedMenusByRoleIds(List.of(2L, 3L));

        assertThat(result).extracting(item -> item.menuId()).containsExactly(9L);
        verify(menuDao).queryAuthorizedMenuListByRoleIds(List.of(2L, 3L));
    }

    @Test
    void listAuthorizedMenusByRoleIdsRejectsEmptyQueryBeforeMapper() {
        assertThat(adapter.listAuthorizedMenusByRoleIds(List.of())).isEmpty();

        verify(menuDao, never())
                .queryAuthorizedMenuListByRoleIds(org.mockito.ArgumentMatchers.anyList());
    }

    private MenuVO menu(Long menuId, String menuName, String apiPerms) {
        MenuVO menu = new MenuVO();
        menu.setMenuId(menuId);
        menu.setMenuName(menuName);
        menu.setApiPerms(apiPerms);
        menu.setDisabledFlag(false);
        return menu;
    }
}
