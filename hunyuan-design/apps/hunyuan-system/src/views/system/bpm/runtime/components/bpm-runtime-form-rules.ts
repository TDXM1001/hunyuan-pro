import type { FormRule } from '@form-create/element-ui';

export interface BpmRuntimeEmployeeOption {
  label: string;
  value: number;
}

type RuntimeRule = FormRule & Record<string, any>;

function isEmployeeSelectRule(rule: RuntimeRule) {
  const values = [
    rule.type,
    rule.component,
    rule.props?.type,
    rule.props?.component,
  ]
    .filter(Boolean)
    .map((value) => String(value).toLowerCase());

  return values.some(
    (value) => value === 'employee' || value === 'employeeselect',
  );
}

export function hasEmployeeSelectRule(rules: FormRule[]) {
  return rules.some((rule) => isEmployeeSelectRule(rule as RuntimeRule));
}

export function normalizeRuntimeFormRules(
  rules: FormRule[],
  employeeOptions: BpmRuntimeEmployeeOption[],
  remoteMethod: (keyword: string) => Promise<void> | void,
): FormRule[] {
  return rules.map((rule) => {
    const runtimeRule = rule as RuntimeRule;
    if (!isEmployeeSelectRule(runtimeRule)) {
      return rule;
    }

    return {
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
    } as FormRule;
  });
}
