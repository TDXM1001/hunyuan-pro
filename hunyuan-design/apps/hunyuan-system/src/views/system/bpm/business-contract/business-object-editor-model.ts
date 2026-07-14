import type { BpmBusinessObjectDraft, BpmBusinessObjectField } from '#/api/system/bpm';

export function createBusinessObjectModel(): BpmBusinessObjectDraft {
  return {
    attachmentRule: { allowedExtensions: ['pdf', 'jpg', 'png'], maxCount: 5, maxSizeMb: 20, required: false },
    businessKeyRule: { datePattern: 'yyyyMMdd', prefix: 'REQ', sequenceDigits: 4 },
    businessType: 'GENERIC_APPLICATION', catalogRevision: 0, contractKey: '', contractVersion: 1,
    dataChangeRule: { editableFields: [], mode: 'FIELD_CONTROLLED' }, description: '', fieldSchema: [],
    objectName: '', routingFacts: [], schemaVersion: 2, sourceSystem: 'HUNYUAN', workingDataSchema: [],
  };
}

export function createBusinessObjectField(): BpmBusinessObjectField {
  return { candidateUsable: false, key: '', label: '', presentation: { control: 'INPUT', options: [], placeholder: '', unit: '' }, required: false, sensitivity: 'INTERNAL', type: 'STRING' };
}

export function moveBusinessObjectField(fields: BpmBusinessObjectField[], index: number, offset: number) {
  const target = index + offset;
  if (target < 0 || target >= fields.length) return;
  const [field] = fields.splice(index, 1);
  if (field) fields.splice(target, 0, field);
}
