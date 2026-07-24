import { describe, expect, it, vi } from 'vitest';

import { createAppFeatureRegistry } from './index';

const loader = vi.fn(async () => ({ default: {} }));

describe('应用模块注册表', () => {
  it('按 routeId 生成稳定组件键', () => {
    const registry = createAppFeatureRegistry([
      {
        feature: {
          id: 'organization',
          routes: [
            { path: '/organization', routeId: 'organization.directory' },
          ],
        },
        routeLoaders: { 'organization.directory': loader },
      },
    ]);

    expect(registry.resolveComponent('organization.directory')).toBe(
      '/__app_kernel__/organization.directory',
    );
    expect(registry.createPageMap()).toEqual({
      '/__app_kernel__/organization.directory.vue': loader,
    });
  });

  it('拒绝重复模块、缺失依赖、重复 routeId 和缺失加载器', () => {
    expect(() =>
      createAppFeatureRegistry([
        { feature: { id: 'same', routes: [] }, routeLoaders: {} },
        { feature: { id: 'same', routes: [] }, routeLoaders: {} },
      ]),
    ).toThrow('模块 ID 重复');

    expect(() =>
      createAppFeatureRegistry([
        {
          feature: { dependencies: ['missing'], id: 'consumer', routes: [] },
          routeLoaders: {},
        },
      ]),
    ).toThrow('缺少依赖');

    expect(() =>
      createAppFeatureRegistry([
        {
          feature: {
            id: 'one',
            routes: [{ path: '/one', routeId: 'shared.route' }],
          },
          routeLoaders: { 'shared.route': loader },
        },
        {
          feature: {
            id: 'two',
            routes: [{ path: '/two', routeId: 'shared.route' }],
          },
          routeLoaders: { 'shared.route': loader },
        },
      ]),
    ).toThrow('routeId 冲突');

    expect(() =>
      createAppFeatureRegistry([
        {
          feature: {
            id: 'missing-loader',
            routes: [{ path: '/missing', routeId: 'missing.route' }],
          },
          routeLoaders: {},
        },
      ]),
    ).toThrow('缺少路由加载器');
  });

  it('关闭模块后不暴露其路由', () => {
    const registry = createAppFeatureRegistry([
      {
        enabled: false,
        feature: {
          id: 'disabled',
          routes: [{ path: '/disabled', routeId: 'disabled.route' }],
        },
        routeLoaders: { 'disabled.route': loader },
      },
    ]);

    expect(registry.hasRoute('disabled.route')).toBe(false);
  });
});
