import { describe, expect, it } from 'vitest';

import {
  buildConfigMutationPayload,
  buildConfigPageQueryPayload,
} from './config';

describe('parameter config api payloads', () => {
  it('trims config query keywords and preserves paging fields', () => {
    expect(
      buildConfigPageQueryPayload({
        configKey: '  system.demo.key  ',
        pageNum: 2,
        pageSize: 20,
      }),
    ).toEqual({
      configKey: 'system.demo.key',
      pageNum: 2,
      pageSize: 20,
    });
  });

  it('omits blank config query keywords after trimming', () => {
    expect(
      buildConfigPageQueryPayload({
        configKey: '   ',
        pageNum: 1,
        pageSize: 10,
      }),
    ).toEqual({
      configKey: undefined,
      pageNum: 1,
      pageSize: 10,
    });
  });

  it('trims config mutation payload fields and preserves configId on update', () => {
    expect(
      buildConfigMutationPayload({
        configId: 9,
        configKey: '  system.welcome.text  ',
        configName: '  欢迎文案  ',
        configValue: '  欢迎使用混元系统  ',
        remark: '  首页配置  ',
      }),
    ).toEqual({
      configId: 9,
      configKey: 'system.welcome.text',
      configName: '欢迎文案',
      configValue: '欢迎使用混元系统',
      remark: '首页配置',
    });
  });
});
