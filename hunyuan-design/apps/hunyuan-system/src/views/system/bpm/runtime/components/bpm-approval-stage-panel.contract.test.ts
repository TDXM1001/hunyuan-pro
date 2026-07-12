import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const root = process.cwd();
const readSource = (path: string) => readFileSync(resolve(root, path), 'utf8');

describe('M2 approval stage runtime detail contract', () => {
  it('renders frozen policy versions, progress, and original/current handlers without engine internals', () => {
    const panel = readSource(
      'apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-approval-stage-panel.vue',
    );
    const drawer = readSource(
      'apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue',
    );

    expect(panel).toContain('BpmApprovalStageTraceRecord');
    expect(panel).toContain('candidatePolicyVersionId');
    expect(panel).toContain('approvalPolicyVersionId');
    expect(panel).toContain('requiredApprovalCount');
    expect(panel).toContain('sourceEmployeeId');
    expect(panel).toContain('currentEmployeeId');
    expect(panel).toContain('sourceEmployeeNameSnapshot');
    expect(panel).toContain('currentEmployeeNameSnapshot');
    expect(panel).toContain('terminalReason');
    expect(panel).not.toContain('engineExecutionId');
    expect(panel).not.toContain('approvalPolicySnapshotJson');
    expect(drawer).toContain('BpmApprovalStagePanel');
    expect(drawer).toContain('approvalStages');
  });
});
