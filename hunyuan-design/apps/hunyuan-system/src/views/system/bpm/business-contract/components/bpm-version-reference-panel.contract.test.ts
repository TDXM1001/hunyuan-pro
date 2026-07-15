import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('business object graph reference panel contract', () => {
  it('uses a published version fallback instead of rendering undefined', () => {
    const source = readFileSync(
      resolve(
        process.cwd(),
        'apps/hunyuan-system/src/views/system/bpm/business-contract/components/bpm-version-reference-panel.vue',
      ),
      'utf8',
    );

    expect(source).toContain('item.definitionVersion ?? item.graphDefinitionVersionId');
    expect(source).not.toContain('v${item.definitionVersion}`');
  });
});
