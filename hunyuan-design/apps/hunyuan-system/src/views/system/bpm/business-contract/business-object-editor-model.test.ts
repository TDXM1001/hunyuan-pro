import { describe, expect, it } from 'vitest';

import { createBusinessObjectModel, moveBusinessObjectField } from './business-object-editor-model';

describe('business object visual editor model', () => {
  it('creates a typed draft without canonical JSON or digest', () => {
    const model = createBusinessObjectModel();
    expect(model).toMatchObject({ schemaVersion: 2, catalogRevision: 0, sourceSystem: 'HUNYUAN' });
    expect(model).not.toHaveProperty('canonicalContractJson');
    expect(model).not.toHaveProperty('contractDigest');
    expect(model.attachmentRule.allowedExtensions).toEqual(['pdf', 'jpg', 'png']);
  });

  it('moves fields with stable array operations', () => {
    const model = createBusinessObjectModel();
    model.fieldSchema.push(
      { key: 'amount', label: '申请金额', type: 'DECIMAL', required: true, sensitivity: 'INTERNAL', candidateUsable: false, presentation: { control: 'NUMBER', placeholder: '', unit: '元', options: [] } },
      { key: 'reason', label: '申请事由', type: 'STRING', required: true, sensitivity: 'INTERNAL', candidateUsable: false, presentation: { control: 'TEXTAREA', placeholder: '', unit: '', options: [] } },
    );
    moveBusinessObjectField(model.fieldSchema, 1, -1);
    expect(model.fieldSchema.map((field) => field.key)).toEqual(['reason', 'amount']);
  });
});
