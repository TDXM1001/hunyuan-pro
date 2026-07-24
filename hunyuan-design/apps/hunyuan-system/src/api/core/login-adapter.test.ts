import { describe, expect, it } from 'vitest';

import { mapLoginMenusToRoutes } from './login-adapter';

describe('登录菜单稳定路由契约', () => {
  it('优先使用 routeId 注册的懒加载组件', () => {
    const routes = mapLoginMenusToRoutes([
      {
        component: '/legacy/not-used.vue',
        menuId: 219,
        menuName: '部门目录',
        menuType: 2,
        parentId: 0,
        path: '/organization/directory',
        routeId: 'organization.department.directory',
      },
    ]);

    const route = routes.find(
      (item) => item.path === '/organization/directory',
    );
    expect(route?.component).toBe(
      '/__app_kernel__/organization.department.directory',
    );
  });

  it('未知 routeId 进入模块桥接页', () => {
    const routes = mapLoginMenusToRoutes([
      {
        component: '/system/employee/index.vue',
        menuId: 900,
        menuName: '已关闭模块',
        menuType: 2,
        parentId: 0,
        path: '/disabled',
        routeId: 'disabled.module.page',
      },
    ]);

    const route = routes.find((item) => item.path === '/disabled');
    expect(route?.component).toBe('/system/module-bridge/index');
  });

  it('没有 routeId 的历史菜单继续按 component 解析', () => {
    const routes = mapLoginMenusToRoutes([
      {
        component: '/system/employee/index.vue',
        menuId: 46,
        menuName: '员工管理',
        menuType: 2,
        parentId: 0,
        path: '/organization/employee',
      },
    ]);

    const route = routes.find((item) => item.path === '/organization/employee');
    expect(route?.component).toBe('/system/employee/index');
  });

  it('受限角色只生成后端授权返回的菜单', () => {
    const routes = mapLoginMenusToRoutes([
      {
        menuId: 76,
        menuName: '角色管理',
        menuType: 2,
        parentId: 0,
        path: '/organization/role',
        routeId: 'access.role.management',
      },
    ]);

    expect(routes.some((item) => item.path === '/organization/role')).toBe(
      true,
    );
    expect(routes.some((item) => item.path === '/system/menu')).toBe(false);
  });
});
