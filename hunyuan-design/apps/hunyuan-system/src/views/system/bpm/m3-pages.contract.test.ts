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
    [
      'createBpmBusinessObjectVisualDraft',
      'saveBpmBusinessObjectVisualDraft',
      'validateBpmBusinessObjectVisualDraft',
      'BpmSchemaFieldTable',
      'Page auto-content-height',
      '!p-3 h-full min-h-0 overflow-hidden',
      'queryDictOptionsByCode',
      'BPM_BUSINESS_TYPE',
      '<ElSelect',
    ].forEach((needle) => expect(editor).toContain(needle));
    expect(editor).not.toContain('<ElInput v-model="model.businessType"');
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
      'configuration?.routingFacts',
      'lineItemSchema',
      'submitBpmGenericApplication',
      'toBusinessObjectFormRules',
      'getBpmBusinessObjectDetail',
    ].forEach((needle) => expect(source).toContain(needle));
    const workbench = readFileSync(resolve(process.cwd(), 'apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-task-form-workbench.vue'), 'utf8');
    expect(workbench).toContain('toBusinessObjectFormRules');
    expect(workbench).toContain('toBusinessObjectLineItemRows');
    expect(workbench).toContain('workingDataSchema.some((field) => field.key === permission.fieldKey)');
    expect(workbench).not.toContain('hasOwnProperty.call(formData.value');
    const todo = readFileSync(resolve(process.cwd(), 'apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue'), 'utf8');
    expect(todo).not.toContain('label="任务标识"');
    expect(todo).not.toContain('运行时分配快照');
    expect(source).not.toContain('attachmentsJson');
    expect(source).not.toContain('lineItemsJson');
    expect(source).not.toContain('流程初始化信息');
    expect(source).not.toContain('workingRules');
  });
});
