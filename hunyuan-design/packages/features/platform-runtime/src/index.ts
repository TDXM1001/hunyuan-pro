import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

// 运行时运维页面共享传输边界，任务、序列号、缓存和刷新项分别保留独立路由能力。
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
