import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('M2 policy catalog management page contract', () => {
  it('manages immutable policy versions through the catalog API', () => {
    const source = readFileSync(resolve(
      process.cwd(),
      'apps/hunyuan-system/src/views/system/bpm/policy/policy-catalog.vue',
    ), 'utf8');

    expect(source).toContain('queryBpmPolicyCatalog');
    expect(source).toContain('validateBpmPolicyDraft');
    expect(source).toContain('createBpmPolicyDraft');
    expect(source).toContain('copyBpmPolicyAsDraft');
    expect(source).toContain('activateBpmPolicyVersion');
    expect(source).toContain('retireBpmPolicyVersion');
    expect(source).toContain('已启用版本只可复制为新草稿');
  });

  it('registers the catalog page and every server permission through the M2 incremental SQL', () => {
    const sql = readFileSync(resolve(
      process.cwd(),
      '../数据库SQL脚本/mysql/sql-update-log/v3.54.0.sql',
    ), 'utf8');

    expect(sql).toContain("(342, '审批策略目录', 2, 308");
    expect(sql).toContain("'/system/bpm/policy/policy-catalog'");
    expect(sql).toContain("'/system/bpm/policy/policy-catalog.vue'");
    expect(sql).toContain("'bpm:policy-catalog:list'");
    expect(sql).toContain("'bpm:policy-catalog:detail'");
    expect(sql).toContain("'bpm:policy-catalog:add'");
    expect(sql).toContain("'bpm:policy-catalog:copy'");
    expect(sql).toContain("'bpm:policy-catalog:activate'");
    expect(sql).toContain("'bpm:policy-catalog:retire'");
    expect(sql).toContain("'BPM_POLICY_TYPE'");
    expect(sql).toContain("'BPM_POLICY_LIFECYCLE_STATE'");
    expect(sql).toContain("'BPM_APPROVAL_COMPLETION_MODE'");
    expect(sql).toContain("'BPM_APPROVAL_STAGE_STATE'");
    expect(sql).toContain("'BPM_APPROVAL_STAGE_MEMBER_STATE'");
  });
});
