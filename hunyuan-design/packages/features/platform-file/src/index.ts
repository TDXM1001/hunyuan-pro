import type { AppFeatureDefinition } from '@hunyuan/app-kernel';

// 文件管理菜单通过稳定 routeId 装配，避免再次依赖应用内的历史页面路径。
export const platformFileFeature = {
  capabilities: ['support:file:query'],
  id: 'platform.file',
  routes: [
    {
      path: '/support/file/file-list',
      routeId: 'platform.file.management',
    },
  ],
} as const satisfies AppFeatureDefinition;

export * from './management/client';
export { platformFileRequestClientKey } from './dependencies';
