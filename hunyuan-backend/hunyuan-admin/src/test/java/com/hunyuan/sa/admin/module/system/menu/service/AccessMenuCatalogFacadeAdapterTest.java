package com.hunyuan.sa.admin.module.system.menu.service;

import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuFailure;
import com.hunyuan.sa.admin.module.access.menu.api.CreateAccessMenuCommand;
import com.hunyuan.sa.admin.module.access.menu.api.UpdateAccessMenuCommand;
import com.hunyuan.sa.admin.module.system.menu.dao.MenuDao;
import com.hunyuan.sa.admin.module.system.menu.domain.entity.MenuEntity;
import com.hunyuan.sa.admin.module.system.menu.domain.vo.MenuVO;
import com.hunyuan.sa.base.common.domain.RequestUrlVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessMenuCatalogFacadeAdapterTest {

    @Mock
    private MenuDao menuDao;

    private AccessMenuCatalogFacadeAdapter adapter;
    private List<RequestUrlVO> authorizationUrls;

    @BeforeEach
    void setUp() {
        authorizationUrls = new ArrayList<>();
        adapter = new AccessMenuCatalogFacadeAdapter();
        ReflectionTestUtils.setField(adapter, "menuDao", menuDao);
        ReflectionTestUtils.setField(adapter, "authUrl", authorizationUrls);
    }

    @Test
    void createRejectsDuplicatedNameBeforeWebPermission() {
        when(menuDao.getByMenuName("用户管理", 1L, false))
                .thenReturn(entity(8L, "用户管理", 1L, false));

        var result = adapter.create(createCommand("用户管理", "system:user"));

        assertThat(result.failure()).isEqualTo(AccessMenuFailure.MENU_NAME_DUPLICATED);
        assertThat(result.message()).isEqualTo("菜单名称已存在");
        verify(menuDao, never()).getByWebPerms(any(), any());
        verify(menuDao, never()).insert(any(MenuEntity.class));
    }

    @Test
    void createRejectsDuplicatedWebPermission() {
        when(menuDao.getByMenuName("用户管理", 1L, false)).thenReturn(null);
        when(menuDao.getByWebPerms("system:user", false))
                .thenReturn(entity(8L, "旧用户管理", 1L, false));

        var result = adapter.create(createCommand("用户管理", "system:user"));

        assertThat(result.failure()).isEqualTo(AccessMenuFailure.WEB_PERMISSION_DUPLICATED);
        assertThat(result.message()).isEqualTo("前端权限字符串已存在");
        verify(menuDao, never()).insert(any(MenuEntity.class));
    }

    @Test
    void createReturnsGeneratedMenuIdAndOperator() {
        when(menuDao.getByMenuName("用户管理", 1L, false)).thenReturn(null);
        when(menuDao.getByWebPerms("system:user", false)).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MenuEntity entity = invocation.getArgument(0);
            entity.setMenuId(9L);
            return 1;
        }).when(menuDao).insert(any(MenuEntity.class));

        var result = adapter.create(createCommand("用户管理", "system:user"));

        assertThat(result.successful()).isTrue();
        assertThat(result.data()).isEqualTo(9L);
        verify(menuDao).insert(org.mockito.ArgumentMatchers.<MenuEntity>argThat(
                entity -> entity.getCreateUserId().equals(100L)
                        && entity.getMenuName().equals("用户管理")));
    }

    @Test
    void updateRejectsMissingAndDeletedMenusBeforeUniquenessChecks() {
        when(menuDao.selectById(9L)).thenReturn(null);

        var missing = adapter.update(updateCommand(9L, 1L));

        assertThat(missing.failure()).isEqualTo(AccessMenuFailure.MENU_NOT_FOUND);
        verify(menuDao, never()).getByMenuName(any(), any(), any());

        when(menuDao.selectById(10L)).thenReturn(entity(10L, "已删除", 1L, true));
        var deleted = adapter.update(updateCommand(10L, 1L));

        assertThat(deleted.failure()).isEqualTo(AccessMenuFailure.MENU_DELETED);
        assertThat(deleted.message()).isEqualTo("菜单已被删除");
    }

    @Test
    void updateRejectsSelfParentAfterUniquenessChecks() {
        MenuEntity current = entity(9L, "用户管理", 1L, false);
        when(menuDao.selectById(9L)).thenReturn(current);
        when(menuDao.getByMenuName("用户管理", 9L, false)).thenReturn(null);
        when(menuDao.getByWebPerms("system:user", false)).thenReturn(current);

        var result = adapter.update(updateCommand(9L, 9L));

        assertThat(result.failure()).isEqualTo(AccessMenuFailure.MENU_PARENT_SELF);
        assertThat(result.message()).isEqualTo("上级菜单不能为自己");
        verify(menuDao, never()).updateById(any(MenuEntity.class));
    }

    @Test
    void deleteRecursivelyDeletesChildrenInLegacyOrder() {
        List<Long> roots = List.of(1L, 2L);
        List<Long> children = List.of(3L, 4L);
        List<Long> grandchildren = List.of(5L);
        when(menuDao.selectMenuIdByParentIdList(roots)).thenReturn(children);
        when(menuDao.selectMenuIdByParentIdList(children)).thenReturn(grandchildren);
        when(menuDao.selectMenuIdByParentIdList(grandchildren)).thenReturn(List.of());

        var result = adapter.delete(roots, 100L);

        assertThat(result.successful()).isTrue();
        InOrder order = inOrder(menuDao);
        order.verify(menuDao).deleteByMenuIdList(roots, 100L, true);
        order.verify(menuDao).selectMenuIdByParentIdList(roots);
        order.verify(menuDao).deleteByMenuIdList(children, 100L, true);
        order.verify(menuDao).selectMenuIdByParentIdList(children);
        order.verify(menuDao).deleteByMenuIdList(grandchildren, 100L, true);
    }

    @Test
    void deleteRejectsEmptyMenuIds() {
        var result = adapter.delete(List.of(), 100L);

        assertThat(result.failure()).isEqualTo(AccessMenuFailure.MENU_IDS_EMPTY);
        assertThat(result.message()).isEqualTo("所选菜单不能为空");
        verify(menuDao, never()).deleteByMenuIdList(any(), any(), any());
    }

    @Test
    void listReturnsOnlyRootReachableMenusInPreorder() {
        MenuVO root = menu(1L, "根菜单", 0L, 1);
        MenuVO child = menu(2L, "子菜单", 1L, 2);
        MenuVO orphan = menu(3L, "孤立菜单", 99L, 2);
        when(menuDao.queryMenuList(false, null, null))
                .thenReturn(new ArrayList<>(List.of(orphan, child, root)));

        var result = adapter.list();

        assertThat(result).extracting(menu -> menu.menuId())
                .containsExactly(1L, 2L);
    }

    @Test
    void treeFiltersFunctionPointsWhenOnlyMenuIsTrue() {
        MenuVO root = menu(1L, "根目录", 0L, 1);
        MenuVO child = menu(2L, "菜单", 1L, 2);
        when(menuDao.queryMenuList(false, null, List.of(1, 2)))
                .thenReturn(List.of(root, child));

        var result = adapter.tree(true);

        assertThat(result).singleElement()
                .satisfies(node -> {
                    assertThat(node.menuId()).isEqualTo(1L);
                    assertThat(node.children()).singleElement()
                            .extracting(childNode -> childNode.menuId())
                            .isEqualTo(2L);
                });
    }

    @Test
    void getRejectsMissingAndDeletedMenu() {
        when(menuDao.selectById(9L)).thenReturn(null);
        when(menuDao.selectById(10L)).thenReturn(entity(10L, "已删除", 0L, true));

        assertThat(adapter.get(9L).failure()).isEqualTo(AccessMenuFailure.MENU_NOT_FOUND);
        assertThat(adapter.get(10L).failure()).isEqualTo(AccessMenuFailure.MENU_DELETED);
    }

    @Test
    void authorizationUrlsArePassedThrough() {
        RequestUrlVO requestUrl = new RequestUrlVO();
        requestUrl.setUrl("/employee/query");
        authorizationUrls.add(requestUrl);

        assertThat(adapter.listAuthorizationUrls()).isSameAs(authorizationUrls);
    }

    private CreateAccessMenuCommand createCommand(String menuName, String webPerms) {
        return new CreateAccessMenuCommand(
                menuName,
                2,
                1L,
                1,
                "/users",
                "system/user/index",
                false,
                null,
                true,
                true,
                false,
                1,
                webPerms,
                null,
                "user",
                null,
                100L);
    }

    private UpdateAccessMenuCommand updateCommand(Long menuId, Long parentId) {
        return new UpdateAccessMenuCommand(
                menuId,
                "用户管理",
                2,
                parentId,
                1,
                "/users",
                "system/user/index",
                false,
                null,
                true,
                true,
                false,
                1,
                "system:user",
                null,
                "user",
                null,
                100L);
    }

    private MenuEntity entity(Long menuId, String menuName, Long parentId, boolean deleted) {
        MenuEntity entity = new MenuEntity();
        entity.setMenuId(menuId);
        entity.setMenuName(menuName);
        entity.setParentId(parentId);
        entity.setDeletedFlag(deleted);
        return entity;
    }

    private MenuVO menu(Long menuId, String menuName, Long parentId, Integer menuType) {
        MenuVO menu = new MenuVO();
        menu.setMenuId(menuId);
        menu.setMenuName(menuName);
        menu.setParentId(parentId);
        menu.setMenuType(menuType);
        return menu;
    }
}
