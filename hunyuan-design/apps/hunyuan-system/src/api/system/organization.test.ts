import { describe, expect, it } from 'vitest';

import { buildEmployeeMutationPayload } from './organization';

describe('organization employee api payloads', () => {
  it('builds an add payload that matches required backend employee fields', () => {
    expect(
      buildEmployeeMutationPayload({
        actualName: ' 张三 ',
        departmentId: 10,
        email: ' zhangsan@example.com ',
        gender: 1,
        loginName: ' zhangsan ',
        phone: ' 13800138000 ',
        positionId: null,
        roleIdList: [1, 3],
      }),
    ).toEqual({
      actualName: '张三',
      departmentId: 10,
      disabledFlag: false,
      email: 'zhangsan@example.com',
      gender: 1,
      loginName: 'zhangsan',
      phone: '13800138000',
      positionId: null,
      roleIdList: [1, 3],
    });
  });

  it('preserves the employee id and disabled flag for update payloads', () => {
    expect(
      buildEmployeeMutationPayload({
        actualName: ' 李四 ',
        departmentId: 20,
        disabledFlag: true,
        email: ' lisi@example.com ',
        employeeId: 7,
        loginName: ' lisi ',
        phone: ' 13900139000 ',
        roleIdList: [],
      }),
    ).toMatchObject({
      actualName: '李四',
      disabledFlag: true,
      email: 'lisi@example.com',
      employeeId: 7,
      loginName: 'lisi',
      phone: '13900139000',
    });
  });
});
