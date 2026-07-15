import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('admin bpm task list contract', () => {
  it('uses admin detail api and hides raw runtime internals', () => {
    const source = readFileSync(
      resolve(
        process.cwd(),
        'apps/hunyuan-system/src/views/system/bpm/task/task-list.vue',
      ),
      'utf8',
    );

    expect(source).toContain('getBpmAdminTaskDetail');
    expect(source).not.toContain('detailData.taskKey');
    expect(source).not.toContain('detailData.runtimeAssignmentSnapshotJson');
  });
});
