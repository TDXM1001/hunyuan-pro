import { describe, expect, it } from 'vitest';

import { buildCacheKeysPath, buildCacheRemovePath } from './cache';

describe('cache api payloads', () => {
  it('builds encoded cache keys and remove paths', () => {
    expect(buildCacheKeysPath(' sys cache ')).toBe(
      '/support/cache/keys/sys%20cache',
    );
    expect(buildCacheRemovePath(' sys cache ')).toBe(
      '/support/cache/remove/sys%20cache',
    );
  });
});
