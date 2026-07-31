import type { InjectionKey } from 'vue';

import type {
  DepartmentOption,
  EmployeeClient,
  PositionOption,
  ReadonlyDirectoryProvider,
} from './contract';

// 员工页同时接收员工写客户端和组织只读目录，拆分注入键可让不同权限边界独立装配。
export const employeeClientKey: InjectionKey<EmployeeClient> = Symbol(
  'identityEmployeeClient',
);

export const employeeDepartmentProviderKey: InjectionKey<
  ReadonlyDirectoryProvider<DepartmentOption>
> = Symbol('identityEmployeeDepartmentProvider');

export const employeePositionProviderKey: InjectionKey<
  ReadonlyDirectoryProvider<PositionOption>
> = Symbol('identityEmployeePositionProvider');
