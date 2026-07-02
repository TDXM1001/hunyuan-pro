import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      affixTab: true,
      icon: 'lucide:settings',
      order: -1,
      title: $t('page.system.title'),
    },
    name: 'System',
    path: '/system',
    children: [
      {
        name: 'SystemHome',
        path: '/system/home',
        component: () => import('#/views/system/home/index.vue'),
        meta: {
          affixTab: true,
          icon: 'lucide:home',
          title: $t('page.system.home'),
        },
      },
    ],
  },
];

export default routes;
