import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

import { describe, expect, it } from 'vitest';

const modulePath = resolve(process.cwd(), 'apps/hunyuan-system/src/api/system/operate-log.ts');

async function loadModule() {
  expect(existsSync(modulePath)).toBe(true);
  return import(pathToFileURL(modulePath).href);
}

describe('operate log api payloads', () => {
  it('trims operate log filters and preserves paging fields', async () => {
    const module = await loadModule();

    expect(
      module.buildOperateLogPageQueryPayload({
        endDate: '2026-07-05',
        keywords: ' 登录 ',
        pageNum: 1,
        pageSize: 10,
        requestKeywords: ' /login ',
        startDate: '2026-07-01',
        successFlag: false,
        userName: ' admin ',
      }),
    ).toEqual({
      endDate: '2026-07-05',
      keywords: '登录',
      pageNum: 1,
      pageSize: 10,
      requestKeywords: '/login',
      startDate: '2026-07-01',
      successFlag: false,
      userName: 'admin',
    });
  });

  it('builds the operate-log detail path from the row id', async () => {
    const module = await loadModule();

    expect(module.buildOperateLogDetailPath(12)).toBe('/support/operateLog/detail/12');
  });
});
