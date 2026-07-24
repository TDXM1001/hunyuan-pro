import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

import {
  buildDictOptionsByCode,
  buildDictDataMutationPayload,
  buildDictMutationPayload,
  buildDictPageQueryPayload,
} from './dict';

const appRelativePath = 'apps/hunyuan-system/src/api/system/dict.ts';
const modulePath = existsSync(resolve(process.cwd(), appRelativePath))
  ? resolve(process.cwd(), appRelativePath)
  : resolve(process.cwd(), 'src/api/system/dict.ts');

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

  it('uses stable platform routes for reads and dictionary creation or updates', () => {
    const source = readFileSync(modulePath, 'utf8');

    expect(source).toContain("'/admin/v1/platform/dictionaries/query'");
    expect(source).toContain('`/admin/v1/platform/dictionaries/${dictId}/items`');
    expect(source).toContain("'/admin/v1/platform/dictionaries/items'");
    expect(source).toContain("'/admin/v1/platform/dictionaries'");
    expect(source).toContain('`/admin/v1/platform/dictionaries/${params.dictId}`');
    expect(source).not.toContain("'/support/dict/queryPage'");
    expect(source).not.toContain("'/support/dict/getAllDictData'");
    expect(source).not.toContain("'/support/dict/add'");
    expect(source).not.toContain("'/support/dict/update'");
    expect(source).not.toContain('`/support/dict/updateDisabled/${dictId}`');
    expect(source).not.toContain("'/support/dict/batchDelete'");
    expect(source).not.toContain('`/support/dict/delete/${dictId}`');
    expect(source).toContain('`/admin/v1/platform/dictionaries/${dictId}/toggle-disabled`');
    expect(source).toContain("'/admin/v1/platform/dictionaries/batch-delete'");
    expect(source).toContain('`/admin/v1/platform/dictionaries/${params.dictId}/items`');
    expect(source).toContain('`/admin/v1/platform/dictionaries/${params.dictId}/items/${params.dictDataId}`');
    expect(source).toContain('`/admin/v1/platform/dictionaries/items/${dictDataId}/toggle-disabled`');
    expect(source).toContain("'/admin/v1/platform/dictionaries/items/batch-delete'");
    expect(source).toContain('`/admin/v1/platform/dictionaries/items/${dictDataId}`');
    expect(source).not.toContain("'/support/dict/dictData/add'");
    expect(source).not.toContain("'/support/dict/dictData/update'");
    expect(source).not.toContain("'/support/dict/dictData/batchDelete'");
  });
});
