import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

export const identityAccountFeature = {
  // 当前账号接口由已登录身份约束，不通过后台菜单能力码单独授权。
  capabilities: [],
  id: 'identity.account',
  routes: [],
} as const satisfies AppFeatureDefinition;

export {
  createIdentityAccountClient,
  type EmployeeAccountProfile,
  type EmployeePasswordChange,
  type EmployeeProfileUpdate,
  type FileUploadResult,
  type IdentityAccountClient,
} from './client';
export { identityAccountClientKey } from './dependencies';
