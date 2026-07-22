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
  id: 'identity.employee',
  route: {
    component: '/system/employee/index.vue',
    path: '/organization/employee',
  },
} as const;

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
