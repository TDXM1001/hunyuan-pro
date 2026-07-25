import { inject } from 'vue';
import type { InjectionKey } from 'vue';
import type { RequestClient } from '@vben/request';

export const platformRuntimeRequestClientKey: InjectionKey<RequestClient> = Symbol('platformRuntimeRequestClient');

/** 运行时运维页面只消费应用注入的传输能力，不依赖应用请求单例。 */
export function usePlatformRuntimeRequestClient() {
  const requestClient = inject(platformRuntimeRequestClientKey);
  if (!requestClient) throw new Error('platform-runtime 请求客户端未由应用装配');
  return requestClient;
}
