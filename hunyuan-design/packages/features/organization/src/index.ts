import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

// 部门和岗位拆成两个可授权 feature，避免只具备员工基础权限的用户看到完整组织管理入口。
export const organizationFeature = {
  capabilities: [
    'organization.department.read',
    'organization.department.create',
    'organization.department.update',
    'organization.department.delete',
  ],
  id: 'organization.directory',
  routes: [
    {
      routeId: 'organization.department.directory',
    },
  ],
} as const satisfies AppFeatureDefinition;

export const organizationPositionFeature = {
  capabilities: [
    'organization.position.read',
    'organization.position.create',
    'organization.position.update',
    'organization.position.delete',
  ],
  id: 'organization.position',
  routes: [
    {
      routeId: 'organization.position.directory',
    },
  ],
} as const satisfies AppFeatureDefinition;

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
