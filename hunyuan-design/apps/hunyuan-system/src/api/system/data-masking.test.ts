import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

import { describe, expect, it } from 'vitest';

const modulePath = resolve(
  process.cwd(),
  'apps/hunyuan-system/src/api/system/data-masking.ts',
);

async function loadModule() {
  expect(existsSync(modulePath)).toBe(true);
  return import(pathToFileURL(modulePath).href);
}

describe('data masking api paths', () => {
  it('保留兼容接口路径并使用验证能力命名', async () => {
    const module = await loadModule();

    expect(module.buildDataMaskingPath()).toBe('/support/dataMasking/demo/query');
    expect(module.queryDataMaskingList).toBeTypeOf('function');
  });
});
