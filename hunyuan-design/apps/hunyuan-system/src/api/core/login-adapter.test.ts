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

  it('F2 平台 feature 均优先解析为已注册的稳定路由', () => {
    const routes = mapLoginMenusToRoutes([
      {
        component: '/support/config/config-list.vue',
        menuId: 109,
        menuName: '参数配置',
        menuType: 2,
        parentId: 0,
        path: '/config/config-list',
        routeId: 'platform.configuration.parameters',
      },
      {
        component: '/support/dict/index.vue',
        menuId: 110,
        menuName: '数据字典',
        menuType: 2,
        parentId: 0,
        path: '/setting/dict',
        routeId: 'platform.configuration.dictionary',
      },
      {
        component: '/support/file/file-list.vue',
        menuId: 193,
        menuName: '文件管理',
        menuType: 2,
        parentId: 0,
        path: '/support/file/file-list',
        routeId: 'platform.file.management',
      },
    ]);

    expect(
      routes.find((route) => route.path === '/config/config-list')?.component,
    ).toBe('/__app_kernel__/platform.configuration.parameters');
    expect(
      routes.find((route) => route.path === '/setting/dict')?.component,
    ).toBe('/__app_kernel__/platform.configuration.dictionary');
    expect(
      routes.find((route) => route.path === '/support/file/file-list')
        ?.component,
    ).toBe('/__app_kernel__/platform.file.management');
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
