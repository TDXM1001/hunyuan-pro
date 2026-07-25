import { describe, expect, it } from 'vitest';
import * as module from './client';

describe('network protect api payloads', () => {
  it('parses config json from the backend config table', async () => {
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
