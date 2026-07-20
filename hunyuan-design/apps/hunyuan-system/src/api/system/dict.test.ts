import { describe, expect, it } from 'vitest';

import {
  buildDictOptionsByCode,
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

  it('builds enabled dictionary options by dict code in sort order', () => {
    expect(
      buildDictOptionsByCode(
        [
          {
            dataLabel: '通用申请',
            dataValue: 'GENERIC_APPLICATION',
            dictCode: 'BUSINESS_TYPE',
            dictDataId: 1,
            dictDisabledFlag: false,
            dictId: 10,
            disabledFlag: false,
            sortOrder: 20,
          },
          {
            dataLabel: '已停用',
            dataValue: 'DISABLED_TYPE',
            dictCode: 'BUSINESS_TYPE',
            dictDataId: 2,
            dictDisabledFlag: false,
            dictId: 10,
            disabledFlag: true,
            sortOrder: 10,
          },
          {
            dataLabel: '其他字典',
            dataValue: 'OTHER',
            dictCode: 'OTHER_DICT',
            dictDataId: 3,
            dictDisabledFlag: false,
            dictId: 11,
            disabledFlag: false,
            sortOrder: 5,
          },
          {
            dataLabel: '差旅报销',
            dataValue: 'EXPENSE',
            dictCode: 'BUSINESS_TYPE',
            dictDataId: 4,
            dictDisabledFlag: false,
            dictId: 10,
            disabledFlag: false,
            sortOrder: 10,
          },
        ],
        '  BUSINESS_TYPE  ',
      ),
    ).toEqual([
      { label: '差旅报销', value: 'EXPENSE' },
      { label: '通用申请', value: 'GENERIC_APPLICATION' },
    ]);
  });
});
