import { describe, expect, it } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import type { RouteRecordStringComponent } from '@vben/types';

import { generateAccessible } from '@vben/access';

describe('backend route layout stripping', () => {
  it('removes nested backend BasicLayout shells for catalog routes with children', async () => {
    const RootLayout = () => Promise.resolve({ default: { name: 'RootLayout' } });
    const BasicLayout = () =>
      Promise.resolve({ default: { name: 'BasicLayoutShell' } });
    const SmsTemplatePage = () =>
      Promise.resolve({ default: { name: 'SmsTemplatePage' } });

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          children: [],
          component: RootLayout,
          name: 'Root',
          path: '/',
        },
      ],
    });

    const backendRoutes: RouteRecordStringComponent[] = [
      {
        children: [
          {
            children: [
              {
                component: '/support/sms/template-list',
                meta: { title: '短信模板' },
                name: 'BackendMenu306',
                path: '/support/sms/template-list',
              },
            ],
            component: 'BasicLayout',
            meta: { title: '短信管理' },
            name: 'BackendMenu305',
            path: '/support/sms',
          },
        ],
        component: 'BasicLayout',
        meta: { title: '系统设置' },
        name: 'BackendMenu50',
        path: '/setting',
      },
    ];

    await generateAccessible('backend', {
      fetchMenuListAsync: async () => backendRoutes,
      layoutMap: {
        BasicLayout,
      },
      pageMap: {
        '/support/sms/template-list.vue': SmsTemplatePage,
      },
      router,
      routes: [],
    });

    const matchedRoutes = router.resolve('/support/sms/template-list').matched;
    const systemSettingRoute = matchedRoutes.find(
      (route) => route.name === 'BackendMenu50',
    );
    const smsCatalogRoute = matchedRoutes.find(
      (route) => route.name === 'BackendMenu305',
    );
    const smsTemplateRoute = matchedRoutes.find(
      (route) => route.name === 'BackendMenu306',
    );

    expect(systemSettingRoute?.components?.default).toBeUndefined();
    expect(smsCatalogRoute?.components?.default).toBeUndefined();
    expect(smsTemplateRoute?.components?.default).toBeTypeOf('function');
  });
});
