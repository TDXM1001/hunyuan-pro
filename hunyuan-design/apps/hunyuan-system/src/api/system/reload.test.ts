import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

import { buildReloadMutationPayload, buildReloadResultPath } from './reload';

describe('reload api payloads', () => {
  it('trims reload mutation payload fields', () => {
    expect(
      buildReloadMutationPayload({
        args: '  force=true  ',
        identification: '  20260704  ',
        tag: '  login-config  ',
      }),
    ).toEqual({
      args: 'force=true',
      identification: '20260704',
      tag: 'login-config',
    });
  });

  it('builds encoded reload result paths', () => {
    expect(buildReloadResultPath(' login config ')).toBe(
      '/admin/v1/platform/runtime/reloads/login%20config/results',
    );
  });

  it('uses stable platform runtime routes without legacy endpoints', () => {
    const apiSource = readFileSync(
      resolve(process.cwd(), 'apps/hunyuan-system/src/api/system/reload.ts'),
      'utf8',
    );

    expect(apiSource).toContain("'/admin/v1/platform/runtime/reloads'");
    expect(apiSource).not.toContain("'/support/reload/");
    expect(apiSource).not.toContain('`/support/reload/');
  });
});
