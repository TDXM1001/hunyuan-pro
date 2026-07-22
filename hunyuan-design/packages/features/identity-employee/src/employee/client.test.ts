import { describe, expect, it } from 'vitest';

import {
  buildDeletePayload,
  buildDepartmentAssignmentPayload,
  buildEmployeeMutationPayload,
  buildEmployeeQueryPayload,
} from './client';

describe('identity employee api payloads', () => {
  it('builds the new employee query contract', () => {
    expect(
      buildEmployeeQueryPayload({
        departmentId: 12,
        disabled: false,
        keyword: ' 张三 ',
        pageNum: 2,
        pageSize: 20,
      }),
    ).toEqual({
      departmentId: 12,
      disabled: false,
      keyword: '张三',
      pageNum: 2,
      pageSize: 20,
    });
  });

  it('builds create and update commands without legacy role fields', () => {
    expect(
      buildEmployeeMutationPayload({
        actualName: ' 张三 ',
        departmentId: 12,
        disabled: false,
        email: ' zhangsan@example.com ',
        loginName: ' zhangsan ',
        phone: ' 13800138000 ',
        positionId: null,
        remark: ' 备注 ',
      }),
    ).toEqual({
      actualName: '张三',
      departmentId: 12,
      disabled: false,
      email: 'zhangsan@example.com',
      loginName: 'zhangsan',
      phone: '13800138000',
      positionId: null,
      remark: '备注',
    });
  });

  it('deduplicates ids for department assignment and deletion', () => {
    expect(
      buildDepartmentAssignmentPayload({
        departmentId: 9,
        employeeIds: [3, 2, 3],
      }),
    ).toEqual({ departmentId: 9, employeeIds: [3, 2] });
    expect(buildDeletePayload([3, 2, 3])).toEqual({ employeeIds: [3, 2] });
  });
});
