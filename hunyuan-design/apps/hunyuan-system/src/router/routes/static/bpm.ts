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
      {
        component: () => import('#/views/system/bpm/policy/policy-editor.vue'),
        meta: { activePath: '/system/bpm/policy/policy-catalog', hideInMenu: true, title: '编辑审批规则' },
        name: 'SystemBpmPolicyEditorRoute',
        path: '/system/bpm/policy/editor',
      },
      {
        component: () => import('#/views/system/bpm/policy/policy-detail.vue'),
        meta: { activePath: '/system/bpm/policy/policy-catalog', hideInMenu: true, title: '审批规则详情' },
        name: 'SystemBpmPolicyDetailRoute',
        path: '/system/bpm/policy/detail',
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
