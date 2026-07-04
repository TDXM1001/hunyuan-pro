import { describe, expect, it } from 'vitest';

import {
  buildDictDataMutationPayload,
  buildDictMutationPayload,
  buildDictPageQueryPayload,
} from './dict';

describe('dictionary api payloads', () => {
  it('trims dictionary page query keywords and preserves paging fields', () => {
    expect(
      buildDictPageQueryPayload({
        disabledFlag: false,
        keywords: '  status  ',
        pageNum: 2,
        pageSize: 20,
      }),
    ).toEqual({
      disabledFlag: false,
      keywords: 'status',
      pageNum: 2,
      pageSize: 20,
    });
  });

  it('omits blank dictionary keywords after trimming', () => {
    expect(
      buildDictPageQueryPayload({
        disabledFlag: undefined,
        keywords: '   ',
        pageNum: 1,
        pageSize: 10,
      }),
    ).toEqual({
      disabledFlag: undefined,
      keywords: undefined,
      pageNum: 1,
      pageSize: 10,
    });
  });

  it('trims dictionary mutation payload fields', () => {
    expect(
      buildDictMutationPayload({
        dictCode: '  sys_status  ',
        dictId: 7,
        dictName: '  状态字典  ',
        remark: '  系统状态项  ',
      }),
    ).toEqual({
      dictCode: 'sys_status',
      dictId: 7,
      dictName: '状态字典',
      remark: '系统状态项',
    });
  });

  it('trims dictionary-item mutation payload fields and preserves ids', () => {
    expect(
      buildDictDataMutationPayload({
        dataLabel: '  启用  ',
        dataStyle: '  success  ',
        dataValue: '  Y  ',
        dictCode: 'sys_status',
        dictDataId: 11,
        dictId: 7,
        remark: '  默认开启  ',
        sortOrder: 99,
      }),
    ).toEqual({
      dataLabel: '启用',
      dataStyle: 'success',
      dataValue: 'Y',
      dictCode: 'sys_status',
      dictDataId: 11,
      dictId: 7,
      remark: '默认开启',
      sortOrder: 99,
    });
  });
});
