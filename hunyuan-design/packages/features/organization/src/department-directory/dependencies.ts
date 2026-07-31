import type { InjectionKey } from 'vue';

import type { OrganizationDepartmentClient } from './contract';

// 部门目录只暴露本 feature 的客户端契约，页面不直接依赖应用层 API 实现。
export const organizationDepartmentClientKey: InjectionKey<OrganizationDepartmentClient> =
  Symbol('organizationDepartmentClient');
