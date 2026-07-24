import { inject } from 'vue';

import type { InjectionKey } from 'vue';

import type { IdentityAccountClient } from './client';

export const identityAccountClientKey: InjectionKey<IdentityAccountClient> = Symbol(
  'identityAccountClient',
);

/**
 * 当前账号页面必须由应用装配请求客户端，避免 feature 自行引用应用内部单例。
 */
export function useIdentityAccountClient() {
  const client = inject(identityAccountClientKey);
  if (!client) {
    throw new Error('identity-account 客户端未由应用装配');
  }
  return client;
}
