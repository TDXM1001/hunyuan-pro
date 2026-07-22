export const accessFeature = {
  capabilities: [
    'access.role.read',
    'access.role.create',
    'access.role.update',
    'access.role.delete',
    'access.role.employee.read',
    'access.role.employee.assign',
    'access.role.employee.remove',
    'access.capability.read',
    'access.capability.grant',
    'access.menu.read',
    'access.menu.create',
    'access.menu.update',
    'access.menu.delete',
    'access.data-scope.read',
    'access.data-scope.update',
  ],
  id: 'access.management',
  routes: [
    {
      component: '/system/role/index.vue',
      path: '/system/role',
    },
    {
      component: '/system/menu/menu-list.vue',
      path: '/system/menu',
    },
  ],
} as const;

export { createAccessClient } from './client';
export type {
  AccessClient,
  AccessDataScopeDefinition,
  AccessDataScopeSetting,
  AccessMenuCommand,
  AccessMenuRecord,
  AccessMenuTreeRecord,
  AccessRoleCapabilityGrant,
  AccessRoleCommand,
  AccessRoleMember,
  AccessRoleMemberQuery,
  AccessRoleRecord,
  PageResult,
} from './contract';
export { accessClientKey } from './dependencies';
