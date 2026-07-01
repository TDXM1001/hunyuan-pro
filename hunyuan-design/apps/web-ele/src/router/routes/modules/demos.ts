import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'ic:baseline-view-in-ar',
      keepAlive: true,
      order: 1000,
      title: $t('demos.title'),
    },
    name: 'Demos',
    path: '/demos',
    children: [
      {
        name: 'TableTest',
        path: '/demos/table-test',
        component: () => import('#/views/demos/table-test.vue'),
        meta: {
          icon: 'lucide:table',
          title: $t('demos.table'),
        },
      },
      {
        name: 'DetailTest',
        path: '/demos/detail-test',
        component: () => import('#/views/demos/detail-test.vue'),
        meta: {
          icon: 'lucide:panel-top-open',
          title: $t('demos.detail'),
        },
      },
      {
        name: 'EditTest',
        path: '/demos/edit-test',
        component: () => import('#/views/demos/edit-test.vue'),
        meta: {
          icon: 'lucide:square-pen',
          title: $t('demos.edit'),
        },
      },
      {
        name: 'NaiveDemos',
        path: '/demos/element',
        component: () => import('#/views/demos/element/index.vue'),
        meta: {
          title: $t('demos.elementPlus'),
        },
      },
      {
        name: 'BasicForm',
        path: '/demos/form',
        component: () => import('#/views/demos/form/basic.vue'),
        meta: {
          title: $t('demos.form'),
        },
      },
    ],
  },
];

export default routes;
