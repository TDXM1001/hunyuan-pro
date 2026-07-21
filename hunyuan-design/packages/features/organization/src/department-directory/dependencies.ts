import type { InjectionKey } from 'vue';

import type { OrganizationDepartmentClient } from './contract';

export const organizationDepartmentClientKey: InjectionKey<OrganizationDepartmentClient> =
  Symbol('organizationDepartmentClient');
