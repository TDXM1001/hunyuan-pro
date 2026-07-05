import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

import { describe, expect, it } from 'vitest';

const modulePath = resolve(process.cwd(), 'apps/hunyuan-system/src/api/system/api-encrypt.ts');

async function loadModule() {
  expect(existsSync(modulePath)).toBe(true);
  return import(pathToFileURL(modulePath).href);
}

describe('api encrypt capability helpers', () => {
  it('trims the live response-encrypt demo payload', async () => {
    const module = await loadModule();

    expect(
      module.buildApiEncryptDemoPayload({
        age: 18,
        name: '  Alice  ',
      }),
    ).toEqual({
      age: 18,
      name: 'Alice',
    });
  });

  it('builds encrypted request envelopes for static contract examples', async () => {
    const module = await loadModule();

    expect(module.buildApiEncryptEnvelope('cipher-text')).toEqual({
      encryptData: 'cipher-text',
    });
  });
});
