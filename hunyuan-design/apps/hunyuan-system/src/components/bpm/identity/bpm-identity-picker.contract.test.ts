import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

describe('BPM identity picker', () => {
  it('uses named remote options and has no raw id input', () => {
    const source = readFileSync(resolve(process.cwd(), 'apps/hunyuan-system/src/components/bpm/identity/bpm-identity-picker.vue'), 'utf8');
    expect(source).toContain('queryBpmIdentityOptions');
    expect(source).toContain('displayName');
    expect(source).not.toContain('请输入ID');
    expect(source).not.toContain('type="number"');
  });
});
