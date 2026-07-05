import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const apiFiles = [
  {
    label: 'category',
    needles: ['/bpm/category/query', '/bpm/category/add', '/bpm/category/update'],
    path: 'apps/hunyuan-system/src/api/system/bpm/category.ts',
  },
  {
    label: 'form',
    needles: [
      '/bpm/form/query',
      '/bpm/form/add',
      '/bpm/form/update',
      'buildEmptyBpmFormDesignerSnapshot',
    ],
    path: 'apps/hunyuan-system/src/api/system/bpm/form.ts',
  },
  {
    label: 'model',
    needles: [
      '/bpm/model/query',
      '/bpm/designer/detail/',
      '/bpm/definition/publish',
      'buildEmptyBpmDesignerDraft',
    ],
    path: 'apps/hunyuan-system/src/api/system/bpm/model.ts',
  },
  {
    label: 'definition',
    needles: ['/bpm/definition/query', '/bpm/definition/detail/'],
    path: 'apps/hunyuan-system/src/api/system/bpm/definition.ts',
  },
  {
    label: 'runtime',
    needles: [
      '/bpm/instance/query',
      '/app/bpm/startable',
      '/app/bpm/task/approve',
      '/app/bpm/task/returnToInitiator',
    ],
    path: 'apps/hunyuan-system/src/api/system/bpm/runtime.ts',
  },
  {
    label: 'listener',
    needles: ['/bpm/listener/query', '/bpm/listener/channelOptions'],
    path: 'apps/hunyuan-system/src/api/system/bpm/listener.ts',
  },
] as const;

describe('bpm api 模块', () => {
  it.each(apiFiles)(
    '让 $label API 模块绑定到后端真实 contract',
    ({ needles, path }) => {
      const filePath = resolve(process.cwd(), path);

      expect(existsSync(filePath)).toBe(true);

      const source = readFileSync(filePath, 'utf8');
      needles.forEach((needle) => expect(source).toContain(needle));
    },
  );
});
