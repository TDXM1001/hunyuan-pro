import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const root = process.cwd();
const readSource = (path: string) => readFileSync(resolve(root, path), 'utf8');

describe('M2 approval stage runtime detail contract', () => {
  it('keeps approval stage detail business readable without leaking engine ids or raw policy versions', () => {
    const panel = readSource(
      'apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-approval-stage-panel.vue',
    );
    const drawer = readSource(
      'apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue',
    );

    expect(panel).toContain('BpmApprovalStageTraceRecord');
    expect(panel).toContain('requiredApprovalCount');
    expect(panel).toContain('sourceEmployeeNameSnapshot');
    expect(panel).toContain('currentEmployeeNameSnapshot');
    expect(panel).not.toContain('candidatePolicyVersionId');
    expect(panel).not.toContain('approvalPolicyVersionId');
    expect(panel).not.toContain('sourceEmployeeId');
    expect(panel).not.toContain('currentEmployeeId');
    expect(panel).not.toContain('authoredNodeId');
    expect(panel).not.toContain('terminalReason');
    expect(panel).not.toContain('engineExecutionId');
    expect(panel).not.toContain('approvalPolicySnapshotJson');
    expect(drawer).toContain('BpmApprovalStagePanel');
    expect(drawer).toContain('approvalStages');
  });
});
