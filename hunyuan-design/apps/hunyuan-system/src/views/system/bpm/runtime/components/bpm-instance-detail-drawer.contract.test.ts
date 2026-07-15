import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('bpm instance detail drawer security contract', () => {
  it('keeps business detail readable without raw snapshots or technical trace sections', () => {
    const source = readFileSync(
      resolve(
        process.cwd(),
        'apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue',
      ),
      'utf8',
    );

    expect(source).not.toContain('currentFormDataSnapshotJson');
    expect(source).not.toContain('callbackRecords.length');
    expect(source).not.toContain('commandRecords.length');
    expect(source).not.toContain('notificationRecords.length');
    expect(source).not.toContain('timeEvents.length');
    expect(source).not.toContain('externalWaits.length');
    expect(source).not.toContain('subProcesses.length');
    expect(source).not.toContain('log.fromAssigneeEmployeeId');
    expect(source).not.toContain('BpmRouteDecisionList');
  });
});
