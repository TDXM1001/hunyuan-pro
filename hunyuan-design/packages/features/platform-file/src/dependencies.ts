import { inject } from 'vue';

import type { InjectionKey } from 'vue';
import type { RequestClient } from '@vben/request';

export const platformFileRequestClientKey: InjectionKey<RequestClient> = Symbol(
  'platformFileRequestClient',
);

/**
 * 文件 feature 由应用注入请求客户端，保证页面不引用应用内部 API 文件。
 */
export function usePlatformFileRequestClient() {
  const requestClient = inject(platformFileRequestClientKey);
  if (!requestClient) {
    throw new Error('platform-file 请求客户端未由应用装配');
  }
  return requestClient;
}
