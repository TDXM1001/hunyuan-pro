import type { FormRule } from '@form-create/element-ui';

export interface BpmRuntimeEmployeeOption {
  label: string;
  value: number;
}

type RuntimeRule = FormRule & Record<string, any>;

function isEmployeeSelectRule(rule: RuntimeRule) {
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
