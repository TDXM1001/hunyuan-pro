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

export { createOrganizationDepartmentClient } from './department-directory/client';
export { organizationDepartmentClientKey } from './department-directory/dependencies';
export type { DepartmentRecord } from './department-directory/contract';
