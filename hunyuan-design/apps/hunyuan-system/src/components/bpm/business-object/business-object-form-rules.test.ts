import { describe, expect, it } from 'vitest';

import { createBusinessObjectModel } from '#/views/system/bpm/business-contract/business-object-editor-model';
import { toBusinessObjectFormRules } from './business-object-form-rules';

describe('business object form preview rules', () => {
  it('maps backend registered controls without exposing protocol fields', () => {
    const detail = { configuration: createBusinessObjectModel() };
    detail.configuration.fieldSchema.push({ key: 'amount', label: '申请金额', type: 'DECIMAL', required: true, sensitivity: 'INTERNAL', candidateUsable: false, presentation: { control: 'NUMBER', placeholder: '请输入金额', unit: '元', options: [] } });
    const rules = toBusinessObjectFormRules(detail as any, 'APPLICANT');
    expect(rules[0]).toMatchObject({ field: 'amount', title: '申请金额', type: 'inputNumber' });
    expect(JSON.stringify(rules)).not.toContain('canonicalPayload');
  });

  it('enforces applicant, approver readonly and approver edit perspectives', () => {
    const detail = { configuration: createBusinessObjectModel() } as any;
    detail.configuration.fieldSchema.push({ key: 'amount', label: '申请金额', type: 'DECIMAL', required: true, sensitivity: 'INTERNAL', candidateUsable: false, presentation: { control: 'NUMBER', placeholder: '', unit: '元', options: [] } });
    detail.configuration.workingDataSchema.push({ key: 'approvalNote', label: '审批意见', type: 'STRING', required: false, sensitivity: 'INTERNAL', candidateUsable: false, presentation: { control: 'TEXTAREA', placeholder: '', unit: '', options: [] } });
    detail.configuration.dataChangeRule.editableFields = ['approvalNote'];
    const applicant = toBusinessObjectFormRules(detail, 'APPLICANT');
    const readonly = toBusinessObjectFormRules(detail, 'APPROVER_READONLY');
    const editable = toBusinessObjectFormRules(detail, 'APPROVER_EDIT');
    expect((applicant.find((rule) => rule.field === 'amount') as any)?.props?.disabled).toBe(false);
    expect(readonly.every((rule) => (rule as any).props?.disabled === true)).toBe(true);
    expect((editable.find((rule) => rule.field === 'approvalNote') as any)?.props?.disabled).toBe(false);
    expect((editable.find((rule) => rule.field === 'amount') as any)?.props?.disabled).toBe(true);
  });
});
