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
      '/support/reload/result/login%20config',
    );
  });
});
