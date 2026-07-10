import { describe, expect, it, vi } from 'vitest';

import {
  hasEmployeeSelectRule,
  normalizeRuntimeFormRules,
} from './bpm-runtime-form-rules';

describe('bpm runtime form rules', () => {
  it('把 employeeSelect 字段归一化为单选员工下拉', () => {
    const remoteMethod = vi.fn();

    const rules = normalizeRuntimeFormRules(
      [
        {
          field: 'approverEmployeeId',
          title: '审批人',
          type: 'employeeSelect',
        } as any,
      ],
      [{ label: '张三', value: 100 }],
      remoteMethod,
    );

    expect(rules).toEqual([
      {
        field: 'approverEmployeeId',
        title: '审批人',
        type: 'select',
        options: [{ label: '张三', value: 100 }],
        props: {
          clearable: true,
          filterable: true,
          multiple: false,
          remote: true,
          remoteMethod,
          reserveKeyword: true,
        },
      },
    ]);
  });

  it('保留普通表单字段不做员工选择归一化', () => {
    const remoteMethod = vi.fn();

    const rules = normalizeRuntimeFormRules(
      [
        {
          field: 'amount',
          title: '金额',
          type: 'inputNumber',
        } as any,
      ],
      [],
      remoteMethod,
    );

    expect(rules).toEqual([
      {
        field: 'amount',
        title: '金额',
        type: 'inputNumber',
      },
    ]);
  });

  it('识别运行时表单是否包含员工选择字段', () => {
    expect(
      hasEmployeeSelectRule([
        {
          field: 'approverEmployeeId',
          title: '审批人',
          type: 'employee',
        } as any,
      ]),
    ).toBe(true);

    expect(
      hasEmployeeSelectRule([
        {
          field: 'amount',
          title: '金额',
          type: 'inputNumber',
        } as any,
      ]),
    ).toBe(false);
  });

  it('递归识别并归一化嵌套员工选择字段', () => {
    const remoteMethod = vi.fn();
    const rules = [
      {
        field: 'approvalGroup',
        title: '审批信息',
        type: 'group',
        children: [
          {
            field: 'approverEmployeeId',
            title: '审批人',
            type: 'employeeSelect',
          },
        ],
      } as any,
    ];

    expect(hasEmployeeSelectRule(rules)).toBe(true);

    const normalizedRules = normalizeRuntimeFormRules(
      rules,
      [{ label: '张三', value: 100 }],
      remoteMethod,
    ) as any[];

    expect(normalizedRules[0].children[0]).toEqual({
      field: 'approverEmployeeId',
      title: '审批人',
      type: 'select',
      options: [{ label: '张三', value: 100 }],
      props: {
        clearable: true,
        filterable: true,
        multiple: false,
        remote: true,
        remoteMethod,
        reserveKeyword: true,
      },
    });
  });
});
