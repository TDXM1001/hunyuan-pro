import type { AppFeatureDefinition } from '@hunyuan/app-kernel';
export const platformDevtoolsFeature = {
  capabilities: ['support:apiEncrypt:test'],
  id: 'platform.devtools',
  routes: [{ routeId: 'platform.devtools.api-encrypt' }],
} as const satisfies AppFeatureDefinition;
export { platformDevtoolsRequestClientKey } from './dependencies';
