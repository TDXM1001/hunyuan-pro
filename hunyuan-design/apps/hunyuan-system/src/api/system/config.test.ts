import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

import {
  buildConfigMutationPayload,
  buildConfigPageQueryPayload,
} from './config';

const appRelativePath = 'apps/hunyuan-system/src/api/system/config.ts';
const modulePath = existsSync(resolve(process.cwd(), appRelativePath))
  ? resolve(process.cwd(), appRelativePath)
  : resolve(process.cwd(), 'src/api/system/config.ts');

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

  it('uses stable platform routes for configuration management', () => {
    const source = readFileSync(modulePath, 'utf8');

    expect(source).toContain("'/admin/v1/platform/configurations/query'");
    expect(source).toContain("'/admin/v1/platform/configurations'");
    expect(source).toContain('`/admin/v1/platform/configurations/${params.configId}`');
    expect(source).not.toContain("'/support/config/query'");
    expect(source).not.toContain("'/support/config/add'");
    expect(source).not.toContain("'/support/config/update'");
  });
});
