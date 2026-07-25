import { describe, expect, it } from 'vitest';

import {
  mapLoginMenusToRoutes,
  mapLoginResultToUserInfo,
} from './login-adapter';

describe('登录菜单稳定路由契约', () => {
  it('优先使用 routeId 注册的懒加载组件', () => {
    const routes = mapLoginMenusToRoutes([
      {
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

  it('未知 routeId 明确进入 404，不回退历史源码路径', () => {
    const routes = mapLoginMenusToRoutes([
      {
        menuId: 900,
        menuName: '已关闭模块',
        menuType: 2,
        parentId: 0,
        path: '/disabled',
        routeId: 'disabled.module.page',
      },
    ]);

    const route = routes.find((item) => item.path === '/disabled');
    expect(route?.component).toBe('/_core/fallback/not-found');
  });

  it('F2 平台 feature 均优先解析为已注册的稳定路由', () => {
    const routes = mapLoginMenusToRoutes([
      {
        menuId: 109,
        menuName: '参数配置',
        menuType: 2,
        parentId: 0,
        path: '/config/config-list',
        routeId: 'platform.configuration.parameters',
      },
      {
        menuId: 110,
        menuName: '数据字典',
        menuType: 2,
        parentId: 0,
        path: '/setting/dict',
        routeId: 'platform.configuration.dictionary',
      },
      {
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

  it('没有 routeId 的本地菜单明确进入 404', () => {
    const routes = mapLoginMenusToRoutes([
      {
        menuId: 46,
        menuName: '员工管理',
        menuType: 2,
        parentId: 0,
        path: '/organization/employee',
      },
    ]);

    const route = routes.find((item) => item.path === '/organization/employee');
    expect(route?.component).toBe('/_core/fallback/not-found');
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

describe('登录用户头像地址适配', () => {
  it('将本地文件服务地址和相对地址统一映射到部署时 API 入口', () => {
    expect(
      mapLoginResultToUserInfo({
        avatar: 'http://198.18.0.1:1024/upload/avatar.png?version=1',
      }).avatar,
    ).toBe('/api/upload/avatar.png?version=1');
    expect(
      mapLoginResultToUserInfo({ avatar: '/upload/avatar.png' }).avatar,
    ).toBe('/api/upload/avatar.png');
  });

  it('保留独立对象存储的 HTTPS 地址', () => {
    expect(
      mapLoginResultToUserInfo({
        avatar: 'https://cdn.example.com/avatar.png',
      }).avatar,
    ).toBe('https://cdn.example.com/avatar.png');
  });
});
