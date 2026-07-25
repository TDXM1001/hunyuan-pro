import { inject } from 'vue';
import type { InjectionKey } from 'vue';
import type { RequestClient } from '@vben/request';

export const platformDevtoolsRequestClientKey: InjectionKey<RequestClient> = Symbol('platformDevtoolsRequestClient');

/** 开发工具页面只消费应用注入的传输能力，不依赖应用请求单例。 */
export function usePlatformDevtoolsRequestClient() {
  const requestClient = inject(platformDevtoolsRequestClientKey);
  if (!requestClient) throw new Error('platform-devtools 请求客户端未由应用装配');
  return requestClient;
}
