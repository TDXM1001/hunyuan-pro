import { describe, expect, it } from 'vitest';

import {
  buildPositionMutationPayload,
  buildRoleDataScopePayload,
  buildRoleEmployeePayload,
  buildRoleEmployeeQueryPayload,
  buildRoleMenuPayload,
  buildRoleMutationPayload,
} from './organization';

describe('organization management api payloads', () => {
  it('builds position payloads with trimmed text fields', () => {
    expect(
      buildPositionMutationPayload({
        positionLevel: ' L2 ',
        positionName: ' 高级工程师 ',
        remark: ' 平台研发 ',
        sort: 3,
      }),
    ).toEqual({
      positionLevel: 'L2',
      positionName: '高级工程师',
      remark: '平台研发',
      sort: 3,
    });
  });

  it('builds role payloads with backend role identity preserved', () => {
    expect(
      buildRoleMutationPayload({
        remark: ' 系统管理员 ',
        roleCode: ' admin ',
        roleId: 1,
        roleName: ' 管理员 ',
      }),
    ).toEqual({
      remark: '系统管理员',
      roleCode: 'admin',
      roleId: 1,
      roleName: '管理员',
    });
  });

  it('builds role menu payloads with backend menu ids preserved', () => {
    expect(
      buildRoleMenuPayload({
        menuIdList: [76, 97, 99],
        roleId: 4,
      }),
    ).toEqual({
      menuIdList: [76, 97, 99],
      roleId: 4,
    });
  });

  it('builds role data scope payloads for backend full replacement', () => {
    expect(
      buildRoleDataScopePayload({
        dataScopeItemList: [{ dataScopeType: 1, viewType: 10 }],
        roleId: 4,
      }),
    ).toEqual({
      dataScopeItemList: [{ dataScopeType: 1, viewType: 10 }],
      roleId: 4,
    });
  });

  it('builds role employee payloads with employee ids de-duplicated', () => {
    expect(
      buildRoleEmployeePayload({
        employeeIdList: [3, 2, 3],
        roleId: 4,
      }),
    ).toEqual({
      employeeIdList: [3, 2],
      roleId: 4,
    });
  });

  it('builds role employee query payloads with backend string role id', () => {
    expect(
      buildRoleEmployeeQueryPayload({
        keywords: ' admin ',
        pageNum: 1,
        pageSize: 10,
        roleId: 4,
      }),
    ).toEqual({
      keywords: 'admin',
      pageNum: 1,
      pageSize: 10,
      roleId: '4',
    });
  });
});
