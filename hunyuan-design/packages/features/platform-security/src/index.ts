import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

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
