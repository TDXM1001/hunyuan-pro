import { inject } from 'vue';

import type { InjectionKey } from 'vue';
import type { RequestClient } from '@vben/request';

export const platformNotificationRequestClientKey: InjectionKey<RequestClient> =
  Symbol('platformNotificationRequestClient');

/** 通知页面只消费应用注入的传输能力，不依赖应用请求单例。 */
export function usePlatformNotificationRequestClient() {
  const requestClient = inject(platformNotificationRequestClientKey);
  if (!requestClient) {
    throw new Error('platform-notification 请求客户端未由应用装配');
  }
  return requestClient;
}
