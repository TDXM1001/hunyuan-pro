import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('M2 policy catalog management page contract', () => {
  it('shows read-only technical panels to technical permission holders and built-in admins', () => {
    const policyDetail = readFileSync(resolve(
      process.cwd(),
      'apps/hunyuan-system/src/views/system/bpm/policy/policy-detail.vue',
    ), 'utf8');
    const objectDetail = readFileSync(resolve(
      process.cwd(),
      'apps/hunyuan-system/src/views/system/bpm/business-contract/business-object-detail.vue',
    ), 'utf8');

    expect(policyDetail).toContain("hasAccessByRoles(['admin'])");
    expect(policyDetail).toContain("hasAccessByCodes(['bpm:policy-catalog:technical'])");
    expect(policyDetail).toContain('ArtDetailPage');
    expect(policyDetail).toContain('ArtDetail');
    expect(policyDetail).toContain('Page auto-content-height');
    expect(policyDetail).toContain('!p-3 h-full min-h-0 overflow-hidden');
    expect(policyDetail).not.toContain('ElDescriptions');
    expect(objectDetail).toContain("hasAccessByRoles(['admin'])");
    expect(objectDetail).toContain("hasAccessByCodes(['bpm:business-contract:technical'])");
  });

  it('navigates to visual pages without exposing JSON editors', () => {
    const source = readFileSync(resolve(
      process.cwd(),
      'apps/hunyuan-system/src/views/system/bpm/policy/policy-catalog.vue',
    ), 'utf8');

    expect(source).toContain('queryBpmPolicyCatalog');
    expect(source).toContain('copyBpmPolicyAsDraft');
    expect(source).toContain('retireBpmPolicyVersion');
    expect(source).toContain('/system/bpm/policy/editor');
    expect(source).toContain('/system/bpm/policy/detail');
    expect(source).toContain('继续编辑');
    expect(source).not.toContain('policyJson');
    expect(source).not.toContain('策略 JSON');
  });

  it('registers visual permissions in the latest incremental SQL', () => {
    const sql = readFileSync(resolve(process.cwd(), '../数据库SQL脚本/mysql/sql-update-log/v3.62.0.sql'), 'utf8');
    expect(sql).toContain("'审批规则'");
    expect(sql).toContain("'bpm:policy-catalog:save'");
    expect(sql).toContain("'bpm:policy-catalog:simulate'");
    expect(sql).toContain("'bpm:policy-catalog:technical'");
    expect(sql).toContain("'bpm:policy-catalog:delete'");
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
