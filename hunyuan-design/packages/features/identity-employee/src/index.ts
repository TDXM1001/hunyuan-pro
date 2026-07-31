import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

// 员工 feature 显式声明组织目录依赖，保证路由装配前先完成只读目录能力注入。
export const identityEmployeeFeature = {
  capabilities: [
    'identity.employee.read',
    'identity.employee.create',
    'identity.employee.update',
    'identity.employee.enable',
    'identity.employee.disable',
    'identity.employee.department.assign',
    'identity.employee.delete',
    'identity.employee.password.reset',
  ],
  dependencies: ['organization.directory', 'organization.position'],
  id: 'identity.employee',
  routes: [
    {
      routeId: 'identity.employee.management',
    },
  ],
} as const satisfies AppFeatureDefinition;

export { createIdentityEmployeeClient } from './employee/client';
export {
  employeeClientKey,
  employeeDepartmentProviderKey,
  employeePositionProviderKey,
} from './employee/dependencies';
export type {
  DepartmentOption,
  EmployeeClient,
  EmployeeCreateCommand,
  EmployeeDeleteCommand,
  EmployeeDepartmentAssignmentCommand,
  EmployeeOneTimeCredential,
  EmployeeQueryParams,
  EmployeeRecord,
  EmployeeSummary,
  EmployeeUpdateCommand,
  PageResult,
  PositionOption,
  ReadonlyDirectoryProvider,
} from './employee/contract';
