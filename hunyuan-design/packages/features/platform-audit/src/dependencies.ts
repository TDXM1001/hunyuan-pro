import { inject } from 'vue';

import type { InjectionKey } from 'vue';
import type { RequestClient } from '@vben/request';

export const platformAuditRequestClientKey: InjectionKey<RequestClient> = Symbol(
  'platformAuditRequestClient',
);

/** 审计页面只消费应用注入的传输能力，不依赖应用请求单例。 */
export function usePlatformAuditRequestClient() {
  const requestClient = inject(platformAuditRequestClientKey);
  if (!requestClient) {
    throw new Error('platform-audit 请求客户端未由应用装配');
  }
  return requestClient;
}
