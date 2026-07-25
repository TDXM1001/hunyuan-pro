import { inject } from 'vue';

import type { InjectionKey } from 'vue';
import type { RequestClient } from '@vben/request';

export const platformSecurityRequestClientKey: InjectionKey<RequestClient> = Symbol(
  'platformSecurityRequestClient',
);

/** 安全页面只消费应用注入的传输能力，不依赖应用请求单例。 */
export function usePlatformSecurityRequestClient() {
  const requestClient = inject(platformSecurityRequestClientKey);
  if (!requestClient) {
    throw new Error('platform-security 请求客户端未由应用装配');
  }
  return requestClient;
}
