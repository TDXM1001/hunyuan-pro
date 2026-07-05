import { requestClient } from '#/api/request';

export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface Level3ProtectConfigFormModel {
  fileDetectFlag: boolean;
  loginActiveTimeoutMinutes: number;
  loginFailLockMinutes: number;
  loginFailMaxTimes: number;
  maxUploadFileSizeMb: number;
  passwordComplexityEnabled: boolean;
  regularChangePasswordMonths: number;
  regularChangePasswordNotAllowRepeatTimes: number;
  twoFactorLoginEnabled: boolean;
}

export interface LoginFailRecord {
  createTime?: null | string;
  lockFlag?: null | number;
  loginFailCount: number;
  loginFailId: number;
  loginLockBeginTime?: null | string;
  loginName: string;
  updateTime?: null | string;
  userId?: null | number;
  userType?: null | number;
}

export interface LoginFailPageQueryParams {
  lockFlag?: boolean;
  loginLockBeginTimeBegin?: null | string;
  loginLockBeginTimeEnd?: null | string;
  loginName?: null | string;
  pageNum: number;
  pageSize: number;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

function createDefaultLevel3ProtectConfig(): Level3ProtectConfigFormModel {
  return {
    fileDetectFlag: false,
    loginActiveTimeoutMinutes: 30,
    loginFailLockMinutes: 30,
    loginFailMaxTimes: 3,
    maxUploadFileSizeMb: 50,
    passwordComplexityEnabled: true,
    regularChangePasswordMonths: 3,
    regularChangePasswordNotAllowRepeatTimes: 3,
    twoFactorLoginEnabled: false,
  };
}

// 后端把三级等保配置以 JSON 字符串存储，这里统一转成前端表单模型。
export function parseLevel3ProtectConfig(
  raw?: null | string,
): Level3ProtectConfigFormModel {
  const defaults = createDefaultLevel3ProtectConfig();

  if (!raw) {
    return defaults;
  }

  try {
    const parsed = JSON.parse(raw);
    return {
      fileDetectFlag: Boolean(parsed.fileDetectFlag),
      loginActiveTimeoutMinutes: Number(
        parsed.loginActiveTimeoutMinutes ?? defaults.loginActiveTimeoutMinutes,
      ),
      loginFailLockMinutes: Number(
        parsed.loginFailLockMinutes ?? defaults.loginFailLockMinutes,
      ),
      loginFailMaxTimes: Number(parsed.loginFailMaxTimes ?? defaults.loginFailMaxTimes),
      maxUploadFileSizeMb: Number(
        parsed.maxUploadFileSizeMb ?? defaults.maxUploadFileSizeMb,
      ),
      passwordComplexityEnabled: Boolean(
        parsed.passwordComplexityEnabled ?? defaults.passwordComplexityEnabled,
      ),
      regularChangePasswordMonths: Number(
        parsed.regularChangePasswordMonths ?? defaults.regularChangePasswordMonths,
      ),
      regularChangePasswordNotAllowRepeatTimes: Number(
        parsed.regularChangePasswordNotAllowRepeatTimes ??
          defaults.regularChangePasswordNotAllowRepeatTimes,
      ),
      twoFactorLoginEnabled: Boolean(parsed.twoFactorLoginEnabled),
    };
  } catch {
    return defaults;
  }
}

export function buildLevel3ProtectConfigPayload(
  params: Level3ProtectConfigFormModel,
): Level3ProtectConfigFormModel {
  return { ...params };
}

export function buildLoginFailPageQueryPayload(params: LoginFailPageQueryParams) {
  return {
    lockFlag: params.lockFlag,
    loginLockBeginTimeBegin: cleanText(params.loginLockBeginTimeBegin) || undefined,
    loginLockBeginTimeEnd: cleanText(params.loginLockBeginTimeEnd) || undefined,
    loginName: cleanText(params.loginName) || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  };
}

export async function queryLevel3ProtectConfig() {
  const raw = await requestClient.get<string>('/support/protect/level3protect/getConfig');
  return parseLevel3ProtectConfig(raw);
}

export async function updateLevel3ProtectConfig(
  params: Level3ProtectConfigFormModel,
) {
  return requestClient.post<string>(
    '/support/protect/level3protect/updateConfig',
    buildLevel3ProtectConfigPayload(params),
  );
}

export async function queryLoginFailPage(params: LoginFailPageQueryParams) {
  return requestClient.post<PageResult<LoginFailRecord>>(
    '/support/protect/loginFail/queryPage',
    buildLoginFailPageQueryPayload(params),
  );
}

export async function batchDeleteLoginFails(loginFailIds: number[]) {
  return requestClient.post<string>('/support/protect/loginFail/batchDelete', loginFailIds);
}
