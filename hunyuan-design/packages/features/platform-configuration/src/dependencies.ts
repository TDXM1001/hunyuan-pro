import { inject } from 'vue';

import type { InjectionKey } from 'vue';
import type { RequestClient } from '@vben/request';

export const platformConfigurationRequestClientKey: InjectionKey<RequestClient> =
  Symbol('platformConfigurationRequestClient');

/**
 * 配置 feature 只能使用应用装配的请求客户端，不能引用应用内部请求模块。
 */
export function usePlatformConfigurationRequestClient() {
  const requestClient = inject(platformConfigurationRequestClientKey);
  if (!requestClient) {
    throw new Error('platform-configuration 请求客户端未由应用装配');
  }
  return requestClient;
}
