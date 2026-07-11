import type { RouteRecordRaw } from 'vue-router';

import { BasicLayout } from '#/layouts';

const routes: RouteRecordRaw[] = [
  {
    component: BasicLayout,
    meta: {
      hideInBreadcrumb: true,
      hideInMenu: true,
      title: 'BpmDesignerShell',
    },
    name: 'SystemBpmDesignerShellRoute',
    path: '/system/bpm/designer-shell',
    children: [
      {
        component: () => import('#/views/system/bpm/form/form-designer.vue'),
        meta: {
          activePath: '/system/bpm/form',
          hideInMenu: true,
          title: '表单设计器',
        },
        name: 'SystemBpmFormDesignerRoute',
        path: '/system/bpm/form/designer',
      },
      {
        component: () => import('#/views/system/bpm/model/model-editor.vue'),
        meta: {
          activePath: '/system/bpm/model',
          hideInMenu: true,
          title: '流程设计器',
        },
        name: 'SystemBpmModelDesignerRoute',
        path: '/system/bpm/model/designer',
      },
    ],
  },
  {
    component: BasicLayout,
    meta: {
      hideInMenu: true,
      title: 'BpmRuntimeShell',
    },
    name: 'SystemBpmRuntimeShellRoute',
    path: '/system/bpm/runtime',
    children: [
      {
        component: () => import('#/views/system/bpm/runtime/start-form.vue'),
        meta: {
          hideInMenu: true,
          title: '流程发起表单',
        },
        name: 'SystemBpmRuntimeStartFormRoute',
        path: 'start-form',
      },
    ],
  },
];

export default routes;
