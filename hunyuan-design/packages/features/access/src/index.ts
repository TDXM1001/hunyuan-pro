import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

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
      path: '/system/role',
      routeId: 'access.role.management',
    },
    {
      path: '/system/menu',
      routeId: 'access.menu.management',
    },
  ],
} as const satisfies AppFeatureDefinition;

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
