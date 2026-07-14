import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

describe('BPM visual configuration routes', () => {
  it('registers independent policy editor and detail routes', () => {
    const source = readFileSync(resolve(process.cwd(), 'apps/hunyuan-system/src/router/routes/static/bpm.ts'), 'utf8');
    expect(source).toContain('/system/bpm/policy/editor');
    expect(source).toContain('/system/bpm/policy/detail');
    expect(source).toContain("activePath: '/system/bpm/policy/policy-catalog'");
  });

  it('registers independent business object editor and detail routes', () => {
    const source = readFileSync(resolve(process.cwd(), 'apps/hunyuan-system/src/router/routes/static/bpm.ts'), 'utf8');
    expect(source).toContain('/system/bpm/business-contract/editor');
    expect(source).toContain('/system/bpm/business-contract/detail');
    expect(source).toContain("activePath: '/system/bpm/business-contract/business-contract-catalog'");
  });
});
