import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

// 安全 feature 同时包含配置、登录失败记录和脱敏验证，能力码用于细分高风险写操作。
export const platformSecurityFeature = {
  capabilities: [
    'support:protect:level3:query', 'support:protect:level3:update',
    'support:protect:loginFail:query', 'support:protect:loginFail:delete',
    'support:dataMasking:query',
  ],
  id: 'platform.security',
  routes: [
    { routeId: 'platform.security.baseline-settings' },
    { routeId: 'platform.security.login-failure' },
    { routeId: 'platform.security.data-masking-validation' },
  ],
} as const satisfies AppFeatureDefinition;

export { platformSecurityRequestClientKey } from './dependencies';
