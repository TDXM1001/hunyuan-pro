import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

import * as module from './client';
const modulePath = resolve(
  process.cwd(),
  'packages/features/platform-audit/src/operation-log/client.ts',
);

describe('operate log api payloads', () => {
  it('trims operate log filters and preserves paging fields', async () => {
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
    expect(module.buildOperateLogDetailPath(12)).toBe(
      '/admin/v1/platform/audit/operation-logs/12',
    );
  });

  it('uses stable platform audit routes', () => {
    const source = readFileSync(modulePath, 'utf8');

    expect(source).toContain("'/admin/v1/platform/audit/operation-logs/query'");
    expect(source).toContain(
      '/admin/v1/platform/audit/operation-logs/${operateLogId}',
    );
    expect(source).not.toContain("'/support/operateLog/page/query'");
    expect(source).not.toContain('/support/operateLog/detail/${operateLogId}');
  });
});
