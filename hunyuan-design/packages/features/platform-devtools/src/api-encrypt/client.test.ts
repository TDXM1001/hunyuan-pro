import { describe, expect, it } from 'vitest';
import * as module from './client';

describe('api encrypt capability helpers', () => {
  it('trims the live response-encrypt demo payload', async () => {
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
    expect(module.buildApiEncryptEnvelope('cipher-text')).toEqual({
      encryptData: 'cipher-text',
    });
  });
});
