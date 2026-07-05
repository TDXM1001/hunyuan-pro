import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

import { describe, expect, it } from 'vitest';

const modulePath = resolve(
  process.cwd(),
  'apps/hunyuan-system/src/api/system/network-protect.ts',
);

async function loadModule() {
  expect(existsSync(modulePath)).toBe(true);
  return import(pathToFileURL(modulePath).href);
}

describe('network protect api payloads', () => {
  it('parses config json from the backend config table', async () => {
    const module = await loadModule();

    expect(
      module.parseLevel3ProtectConfig(
        '{"fileDetectFlag":true,"loginActiveTimeoutMinutes":30,"loginFailLockMinutes":15,"loginFailMaxTimes":3,"maxUploadFileSizeMb":20,"passwordComplexityEnabled":true,"regularChangePasswordMonths":3,"regularChangePasswordNotAllowRepeatTimes":2,"twoFactorLoginEnabled":false}',
      ),
    ).toEqual({
      fileDetectFlag: true,
      loginActiveTimeoutMinutes: 30,
      loginFailLockMinutes: 15,
      loginFailMaxTimes: 3,
      maxUploadFileSizeMb: 20,
      passwordComplexityEnabled: true,
      regularChangePasswordMonths: 3,
      regularChangePasswordNotAllowRepeatTimes: 2,
      twoFactorLoginEnabled: false,
    });
  });

  it('trims and preserves config payload booleans and numbers', async () => {
    const module = await loadModule();

    expect(
      module.buildLevel3ProtectConfigPayload({
        fileDetectFlag: true,
        loginActiveTimeoutMinutes: 30,
        loginFailLockMinutes: 15,
        loginFailMaxTimes: 3,
        maxUploadFileSizeMb: 20,
        passwordComplexityEnabled: true,
        regularChangePasswordMonths: 3,
        regularChangePasswordNotAllowRepeatTimes: 2,
        twoFactorLoginEnabled: false,
      }),
    ).toEqual({
      fileDetectFlag: true,
      loginActiveTimeoutMinutes: 30,
      loginFailLockMinutes: 15,
      loginFailMaxTimes: 3,
      maxUploadFileSizeMb: 20,
      passwordComplexityEnabled: true,
      regularChangePasswordMonths: 3,
      regularChangePasswordNotAllowRepeatTimes: 2,
      twoFactorLoginEnabled: false,
    });
  });

  it('trims login-fail query keywords and preserves paging fields', async () => {
    const module = await loadModule();

    expect(
      module.buildLoginFailPageQueryPayload({
        lockFlag: true,
        loginLockBeginTimeBegin: '2026-07-01',
        loginLockBeginTimeEnd: '2026-07-05',
        loginName: '  admin  ',
        pageNum: 2,
        pageSize: 20,
      }),
    ).toEqual({
      lockFlag: true,
      loginLockBeginTimeBegin: '2026-07-01',
      loginLockBeginTimeEnd: '2026-07-05',
      loginName: 'admin',
      pageNum: 2,
      pageSize: 20,
    });
  });

  it('keeps blank login names out of login-fail queries', async () => {
    const module = await loadModule();

    expect(
      module.buildLoginFailPageQueryPayload({
        lockFlag: undefined,
        loginLockBeginTimeBegin: '',
        loginLockBeginTimeEnd: '',
        loginName: '   ',
        pageNum: 1,
        pageSize: 10,
      }),
    ).toEqual({
      lockFlag: undefined,
      loginLockBeginTimeBegin: undefined,
      loginLockBeginTimeEnd: undefined,
      loginName: undefined,
      pageNum: 1,
      pageSize: 10,
    });
  });
});
