import type { InjectionKey } from 'vue';

import type {
  DepartmentOption,
  EmployeeClient,
  PositionOption,
  ReadonlyDirectoryProvider,
} from './contract';

export const employeeClientKey: InjectionKey<EmployeeClient> = Symbol(
  'identityEmployeeClient',
);

export const employeeDepartmentProviderKey: InjectionKey<
  ReadonlyDirectoryProvider<DepartmentOption>
> = Symbol('identityEmployeeDepartmentProvider');

export const employeePositionProviderKey: InjectionKey<
  ReadonlyDirectoryProvider<PositionOption>
> = Symbol('identityEmployeePositionProvider');
