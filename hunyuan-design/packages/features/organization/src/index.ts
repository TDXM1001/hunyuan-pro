export const organizationFeature = {
  capabilities: [
    'organization.department.read',
    'organization.department.create',
    'organization.department.update',
    'organization.department.delete',
  ],
  id: 'organization.directory',
  route: {
    component: '/organization/directory/index',
    path: '/organization/directory',
  },
} as const;

export const organizationPositionFeature = {
  capabilities: [
    'organization.position.read',
    'organization.position.create',
    'organization.position.update',
    'organization.position.delete',
  ],
  id: 'organization.position',
  route: {
    component: '/system/position/position-list.vue',
    path: '/organization/position',
  },
} as const;

export { createOrganizationDepartmentClient } from './department-directory/client';
export type { DepartmentRecord } from './department-directory/contract';
export { organizationDepartmentClientKey } from './department-directory/dependencies';
export { createOrganizationPositionClient } from './position-directory/client';
export type {
  OrganizationPositionClient,
  PositionCommand,
  PositionRecord,
} from './position-directory/contract';
export { organizationPositionClientKey } from './position-directory/dependencies';
