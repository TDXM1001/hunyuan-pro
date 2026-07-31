import type { InjectionKey } from 'vue';

import type { AccessClient } from './contract';

// 访问控制页面通过应用入口注入客户端，feature 不反向依赖应用内部请求模块。
export const accessClientKey: InjectionKey<AccessClient> =
  Symbol('accessClient');
