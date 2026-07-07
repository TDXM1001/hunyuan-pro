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
  {
    component: () => import('#/views/system/bpm/runtime/startable-list.vue'),
    meta: {
      hideInMenu: true,
      title: '可发起流程',
    },
    name: 'SystemBpmRuntimeStartableRoute',
    path: '/system/bpm/runtime/startable-list',
  },
  {
    component: () => import('#/views/system/bpm/runtime/my-instance-list.vue'),
    meta: {
      hideInMenu: true,
      title: '我的申请',
    },
    name: 'SystemBpmRuntimeMyInstanceRoute',
    path: '/system/bpm/runtime/my-instance-list',
  },
  {
    component: () => import('#/views/system/bpm/runtime/my-todo-list.vue'),
    meta: {
      hideInMenu: true,
      title: '我的待办',
    },
    name: 'SystemBpmRuntimeMyTodoRoute',
    path: '/system/bpm/runtime/my-todo-list',
  },
  {
    component: () => import('#/views/system/bpm/runtime/my-done-list.vue'),
    meta: {
      hideInMenu: true,
      title: '我的已办',
    },
    name: 'SystemBpmRuntimeMyDoneRoute',
    path: '/system/bpm/runtime/my-done-list',
  },
];

export default routes;
