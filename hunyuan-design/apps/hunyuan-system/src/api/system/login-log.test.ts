import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

import { describe, expect, it } from 'vitest';

const modulePath = resolve(process.cwd(), 'apps/hunyuan-system/src/api/system/login-log.ts');

async function loadModule() {
  expect(existsSync(modulePath)).toBe(true);
  return import(pathToFileURL(modulePath).href);
}

describe('login log api payloads', () => {
  it('trims login log filters and preserves dates', async () => {
    const module = await loadModule();

    expect(
      module.buildLoginLogPageQueryPayload({
        endDate: '2026-07-05',
        ip: ' 10.0.0.8 ',
        pageNum: 2,
        pageSize: 20,
        startDate: '2026-07-01',
        userName: ' admin ',
      }),
    ).toEqual({
      endDate: '2026-07-05',
      ip: '10.0.0.8',
      pageNum: 2,
      pageSize: 20,
      startDate: '2026-07-01',
      userName: 'admin',
    });
  });
});
