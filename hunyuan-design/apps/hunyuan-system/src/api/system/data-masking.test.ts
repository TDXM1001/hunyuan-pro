import { existsSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

import { describe, expect, it } from 'vitest';

import { resolveWorkspacePath } from '../../test-utils/workspace-path';

const modulePath = resolveWorkspacePath(
  'apps/hunyuan-system/src/api/system/data-masking.ts',
);

async function loadModule() {
  expect(existsSync(modulePath)).toBe(true);
  return import(pathToFileURL(modulePath).href);
}

describe('data masking api paths', () => {
  it('保留兼容接口路径并使用验证能力命名', async () => {
    const module = await loadModule();

    expect(module.buildDataMaskingPath()).toBe(
      '/support/dataMasking/demo/query',
    );
    expect(module.queryDataMaskingList).toBeTypeOf('function');
  });
});
