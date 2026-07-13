import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('bpm instance detail drawer security contract', () => {
  it('shows subprocess failure policy only in the admin detail source', () => {
    const source = readFileSync(
      resolve(
        process.cwd(),
        'apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue',
      ),
      'utf8',
    );

    expect(source).toContain(
      '<ElTableColumn v-if="detailSource === \'admin\'" label="失败策略"',
    );
  });
});
