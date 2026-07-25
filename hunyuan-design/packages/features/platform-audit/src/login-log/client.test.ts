import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

import * as module from './client';
const modulePath = resolve(
  process.cwd(),
  'packages/features/platform-audit/src/login-log/client.ts',
);

describe('login log api payloads', () => {
  it('trims login log filters and preserves dates', async () => {
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

  it('uses the stable platform login-log route', () => {
    const source = readFileSync(modulePath, 'utf8');

    expect(source).toContain("'/admin/v1/platform/audit/login-logs/query'");
    expect(source).not.toContain("'/support/loginLog/page/query'");
  });
});
