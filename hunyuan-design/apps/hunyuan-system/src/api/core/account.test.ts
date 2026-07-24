import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const modulePath = resolve(process.cwd(), 'src/api/core/account.ts');

describe('账号中心文件契约', () => {
  it('使用稳定文件上传路由而不是历史支持路由', () => {
    const source = readFileSync(modulePath, 'utf8');

    expect(source).toContain("'/admin/v1/platform/files'");
    expect(source).not.toContain("'/support/file/upload'");
  });
});
