import type { InjectionKey } from 'vue';

import type { OrganizationPositionClient } from './contract';

// 岗位目录通过独立注入键装配，避免和部门目录或员工页面共享隐式请求实例。
export const organizationPositionClientKey: InjectionKey<OrganizationPositionClient> =
  Symbol('organizationPositionClient');
