import { describe, expect, it } from 'vitest';
import * as module from './client';

describe('data masking api paths', () => {
  it('保留兼容接口路径并使用验证能力命名', async () => {
    expect(module.buildDataMaskingPath()).toBe(
      '/support/dataMasking/demo/query',
    );
    expect(module.queryDataMaskingList).toBeTypeOf('function');
  });
});
