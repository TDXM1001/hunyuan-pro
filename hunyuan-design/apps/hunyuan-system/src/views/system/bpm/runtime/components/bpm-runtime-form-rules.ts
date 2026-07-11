import type { FormRule } from '@form-create/element-ui';

export interface BpmRuntimeEmployeeOption {
  label: string;
  value: number;
}

export interface BpmRuntimeFieldPermission {
  fieldKey: string;
  permission: 'EDITABLE' | 'HIDDEN' | 'READONLY';
  required: boolean;
}

type RuntimeRule = FormRule & Record<string, any>;

function getNestedRules(
  rule: RuntimeRule,
  fieldName: 'children' | 'fields',
): FormRule[] {
  const value = rule[fieldName];
  return Array.isArray(value) ? (value as FormRule[]) : [];
}

function isEmployeeSelectRule(rule: RuntimeRule): boolean {
  const props =
    rule.props && typeof rule.props === 'object'
      ? (rule.props as Record<string, any>)
      : {};
  const values = [
    (rule as Record<string, any>).type,
    (rule as Record<string, any>).component,
    props.type,
    props.component,
  ]
    .filter(Boolean)
    .map((value) => String(value).toLowerCase());

  return values.some(
    (value) => value === 'employee' || value === 'employeeselect',
  );
}

export function hasEmployeeSelectRule(rules: FormRule[]): boolean {
  return rules.some((rule) => {
    const runtimeRule = rule as RuntimeRule;
    return (
      isEmployeeSelectRule(runtimeRule) ||
      hasEmployeeSelectRule(getNestedRules(runtimeRule, 'children')) ||
      hasEmployeeSelectRule(getNestedRules(runtimeRule, 'fields'))
    );
  });
}

export function normalizeRuntimeFormRules(
  rules: FormRule[],
  employeeOptions: BpmRuntimeEmployeeOption[],
  remoteMethod: (keyword: string) => Promise<void> | void,
  fieldPermissions: BpmRuntimeFieldPermission[] = [],
): FormRule[] {
  const permissionMap = new Map(
    fieldPermissions.map((permission) => [permission.fieldKey, permission]),
  );
  return rules.flatMap((rule) => {
    const runtimeRule = rule as RuntimeRule;
    const fieldKey = String(runtimeRule.field || '');
    const fieldPermission = permissionMap.get(fieldKey);
    if (fieldPermission?.permission === 'HIDDEN') {
      return [];
    }
    const normalizedRule = isEmployeeSelectRule(runtimeRule)
      ? ({
          ...runtimeRule,
          options: employeeOptions,
          props: {
            ...(runtimeRule.props ?? {}),
            clearable: true,
            filterable: true,
            multiple: false,
            remote: true,
            remoteMethod,
            reserveKeyword: true,
          },
          type: 'select',
        } as RuntimeRule)
      : { ...runtimeRule };

    if (fieldPermission?.permission === 'READONLY') {
      normalizedRule.props = {
        ...(normalizedRule.props ?? {}),
        disabled: true,
      };
    }
    if (
      fieldPermission?.permission === 'EDITABLE' &&
      fieldPermission.required
    ) {
      normalizedRule.validate = [
        ...(Array.isArray(normalizedRule.validate)
          ? normalizedRule.validate
          : []),
        { message: `${normalizedRule.title || fieldKey}不能为空`, required: true },
      ];
    }

    if (Array.isArray(runtimeRule.children)) {
      normalizedRule.children = normalizeRuntimeFormRules(
        getNestedRules(runtimeRule, 'children'),
        employeeOptions,
        remoteMethod,
        fieldPermissions,
      );
    }
    if (Array.isArray(runtimeRule.fields)) {
      normalizedRule.fields = normalizeRuntimeFormRules(
        getNestedRules(runtimeRule, 'fields'),
        employeeOptions,
        remoteMethod,
        fieldPermissions,
      );
    }

    return [normalizedRule as FormRule];
  });
}
