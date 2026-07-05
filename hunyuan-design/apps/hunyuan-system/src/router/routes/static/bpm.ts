import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    component: () => import('#/views/system/bpm/form/form-designer.vue'),
    meta: {
      hideInMenu: true,
      title: '表单设计器',
    },
    name: 'SystemBpmFormDesignerRoute',
    path: '/system/bpm/form/designer',
  },
  {
    component: () => import('#/views/system/bpm/model/model-editor.vue'),
    meta: {
      hideInMenu: true,
      title: '流程设计器',
    },
    name: 'SystemBpmModelDesignerRoute',
    path: '/system/bpm/model/designer',
  },
];

export default routes;
