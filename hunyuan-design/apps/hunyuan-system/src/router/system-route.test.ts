import { describe, expect, it } from 'vitest';

import { coreRoutes } from './routes/core';

describe('系统基础路由', () => {
  it('注册个人中心路由供账号菜单跳转', () => {
    const profileRoute = coreRoutes.find((route) => route.name === 'Profile');

    expect(profileRoute).toMatchObject({
      name: 'Profile',
      path: '/profile',
      meta: {
        hideInMenu: true,
      },
    });
    expect(profileRoute?.component).toBeTypeOf('function');
  });
});
