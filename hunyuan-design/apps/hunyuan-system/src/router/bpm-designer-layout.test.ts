import { describe, expect, it } from 'vitest';
import type { RouteRecordRaw } from 'vue-router';

import { BasicLayout } from '#/layouts';

import routes from './routes/static/bpm';

function findRouteWithAncestors(
  records: RouteRecordRaw[],
  name: string,
  ancestors: RouteRecordRaw[] = [],
): { ancestors: RouteRecordRaw[]; route: RouteRecordRaw } | undefined {
  for (const route of records) {
    if (route.name === name) {
      return { ancestors, route };
    }

    const child = findRouteWithAncestors(route.children || [], name, [
      ...ancestors,
      route,
    ]);
    if (child) {
      return child;
    }
  }
}

describe('bpm hidden business routes', () => {
  it.each([
    {
      activePath: '/system/bpm/form',
      name: 'SystemBpmFormDesignerRoute',
      path: '/system/bpm/form/designer?formId=4',
    },
    {
      activePath: '/system/bpm/model',
      name: 'SystemBpmModelDesignerRoute',
      path: '/system/bpm/model/designer?modelId=8',
    },
  ])('keeps $name inside BasicLayout with its menu context', (scenario) => {
    const match = findRouteWithAncestors(routes, scenario.name);

    expect(
      match?.ancestors.some((route) => route.component === BasicLayout),
    ).toBe(true);
    expect(match?.route.path).toBe(scenario.path.split('?')[0]);
    expect(match?.route.meta?.activePath).toBe(scenario.activePath);
    expect(match?.route.meta?.hideInMenu).toBe(true);
  });
});
