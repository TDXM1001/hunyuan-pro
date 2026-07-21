import { describe, expect, it } from 'vitest';

import { resolveResponseErrorMessage } from './response-error';

describe('response error message', () => {
  it('uses the backend ResponseDTO business message', () => {
    expect(
      resolveResponseErrorMessage({
        code: 41004,
        error: 'fallback',
        msg: '请先删除子部门',
      }),
    ).toBe('请先删除子部门');
  });

  it('keeps compatibility with common error fields', () => {
    expect(resolveResponseErrorMessage({ error: 'bad request' })).toBe(
      'bad request',
    );
    expect(resolveResponseErrorMessage({ message: 'network error' })).toBe(
      'network error',
    );
  });
});
