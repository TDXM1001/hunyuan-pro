import type { FormRule } from '@form-create/element-ui';
import type { BpmBusinessObjectDetail, BpmBusinessObjectPerspective } from '#/api/system/bpm';

const controlType: Record<string, string> = { BOOLEAN: 'switch', DATE: 'datePicker', DATETIME: 'datePicker', DECIMAL: 'inputNumber', EMPLOYEE_ID: 'select', INTEGER: 'inputNumber', STRING: 'input' };

export function toBusinessObjectFormRules(detail: BpmBusinessObjectDetail, perspective: BpmBusinessObjectPerspective): FormRule[] {
  const configuration = detail.configuration;
  if (!configuration) return [];
  const fields = perspective === 'APPLICANT' ? configuration.fieldSchema : [...configuration.fieldSchema, ...configuration.workingDataSchema];
  const editable = new Set(configuration.dataChangeRule.editableFields);
  return fields.map((field) => ({
    field: field.key, title: field.label, type: controlType[field.type] || 'input',
    props: { disabled: perspective === 'APPROVER_READONLY' || (perspective === 'APPROVER_EDIT' && !editable.has(field.key)), placeholder: field.presentation.placeholder },
    validate: field.required ? [{ message: `${field.label}不能为空`, required: true }] : [],
  } as FormRule));
}
