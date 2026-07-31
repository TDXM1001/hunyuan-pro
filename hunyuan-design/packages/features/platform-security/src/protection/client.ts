import type { RequestClient } from '@vben/request';

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
    // 配置损坏或历史格式不兼容时回退默认值，保证设置页仍可编辑并重新保存。
    return defaults;
  }
}

export function buildLevel3ProtectConfigPayload(
  params: Level3ProtectConfigFormModel,
): Level3ProtectConfigFormModel {
  // 表单模型已经是后端可接受的字段形状，这里复制对象以避免请求层意外修改响应式表单。
  return { ...params };
}

export function buildLoginFailPageQueryPayload(params: LoginFailPageQueryParams) {
  // 登录失败查询把空的登录名和日期转换为未设置条件，保留锁定状态和分页字段。
  return {
    lockFlag: params.lockFlag,
    loginLockBeginTimeBegin: cleanText(params.loginLockBeginTimeBegin) || undefined,
    loginLockBeginTimeEnd: cleanText(params.loginLockBeginTimeEnd) || undefined,
    loginName: cleanText(params.loginName) || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
  };
}

export async function queryLevel3ProtectConfig(requestClient: RequestClient) {
  // 读取后端保存的三级等保配置，并把 JSON 字符串解析成表单模型。
  const raw = await requestClient.get<string>('/support/protect/level3protect/getConfig');
  return parseLevel3ProtectConfig(raw);
}

export async function updateLevel3ProtectConfig(
  requestClient: RequestClient,
  params: Level3ProtectConfigFormModel,
) {
  // 保存设置页的完整配置；后端负责最终持久化和安全校验。
  return requestClient.post<string>(
    '/support/protect/level3protect/updateConfig',
    buildLevel3ProtectConfigPayload(params),
  );
}

export async function queryLoginFailPage(
  requestClient: RequestClient,
  params: LoginFailPageQueryParams,
) {
  // 分页查询登录失败记录，页面用它展示锁定状态和失败次数。
  return requestClient.post<PageResult<LoginFailRecord>>(
    '/support/protect/loginFail/queryPage',
    buildLoginFailPageQueryPayload(params),
  );
}

export async function batchDeleteLoginFails(
  requestClient: RequestClient,
  loginFailIds: number[],
) {
  // 批量清理登录失败记录，主键数组来自用户明确勾选的记录。
  return requestClient.post<string>('/support/protect/loginFail/batchDelete', loginFailIds);
}
