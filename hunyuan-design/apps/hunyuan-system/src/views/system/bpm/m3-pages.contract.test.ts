import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('BPM M3 页面 contract', () => {
  it('业务对象目录使用独立可视化页面而非 JSON 弹窗', () => {
    const path = resolve(
      process.cwd(),
      'apps/hunyuan-system/src/views/system/bpm/business-contract/business-contract-catalog.vue',
    );
    expect(existsSync(path)).toBe(true);
    const source = readFileSync(path, 'utf8');
    [
      'queryBpmBusinessContracts',
      '/system/bpm/business-contract/editor',
      '/system/bpm/business-contract/detail',
      'copyBpmBusinessContractAsDraft',
      'activateBpmBusinessContract',
      'retireBpmBusinessContract',
    ].forEach((needle) => expect(source).toContain(needle));
    expect(source).not.toContain('契约 JSON');
    expect(source).not.toContain('canonicalContractJson');
    const editor = readFileSync(resolve(process.cwd(), 'apps/hunyuan-system/src/views/system/bpm/business-contract/business-object-editor.vue'), 'utf8');
    ['createBpmBusinessObjectVisualDraft', 'saveBpmBusinessObjectVisualDraft', 'validateBpmBusinessObjectVisualDraft', 'BpmSchemaFieldTable'].forEach((needle) => expect(editor).toContain(needle));
    expect(editor).not.toContain('canonicalContractJson');
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
