import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('BPM M3 页面 contract', () => {
  it('业务契约目录覆盖版本治理动作', () => {
    const path = resolve(
      process.cwd(),
      'apps/hunyuan-system/src/views/system/bpm/business-contract/business-contract-catalog.vue',
    );
    expect(existsSync(path)).toBe(true);
    const source = readFileSync(path, 'utf8');
    [
      'queryBpmBusinessContracts',
      'validateBpmBusinessContract',
      'createBpmBusinessContractDraft',
      'copyBpmBusinessContractAsDraft',
      'activateBpmBusinessContract',
      'retireBpmBusinessContract',
    ].forEach((needle) => expect(source).toContain(needle));
  });

  it('通用申请从契约 schema 生成数据并走统一提交协议', () => {
    const path = resolve(
      process.cwd(),
      'apps/hunyuan-system/src/views/system/bpm/runtime/generic-application.vue',
    );
    expect(existsSync(path)).toBe(true);
    const source = readFileSync(path, 'utf8');
    [
      'queryGenericApplicationContracts',
      'queryBpmStartableDefinitions',
      'contractDocument.fieldSchema',
      'contractDocument.routingFacts',
      'contractDocument.workingDataSchema',
      'submitBpmGenericApplication',
    ].forEach((needle) => expect(source).toContain(needle));
  });
});
