import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

export const platformRuntimeFeature = {
  capabilities: ['support:job:query', 'support:serialNumber:query', 'support:cache:query', 'support:reload:query'],
  id: 'platform.runtime',
  routes: [
    { routeId: 'platform.runtime.job' },
    { routeId: 'platform.runtime.serial-number' },
    { routeId: 'platform.runtime.cache' },
    { routeId: 'platform.runtime.reload' },
  ],
} as const satisfies AppFeatureDefinition;
export { platformRuntimeRequestClientKey } from './dependencies';
