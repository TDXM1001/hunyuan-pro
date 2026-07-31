import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

// 开发工具只提供报文形状验证入口，不在前端持有真实加密密钥或替代后端加解密流程。
export const platformDevtoolsFeature = {
  capabilities: ['support:apiEncrypt:test'],
  id: 'platform.devtools',
  routes: [{ routeId: 'platform.devtools.api-encrypt' }],
} as const satisfies AppFeatureDefinition;
export { platformDevtoolsRequestClientKey } from './dependencies';
