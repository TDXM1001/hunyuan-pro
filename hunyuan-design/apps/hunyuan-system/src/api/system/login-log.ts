import { requestClient } from '#/api/request';

export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface LoginLogRecord {
  createTime?: null | string;
  loginDevice?: null | string;
  loginIp?: null | string;
  loginIpRegion?: null | string;
  loginLogId: number;
  loginResult?: null | number;
  remark?: null | string;
  userAgent?: null | string;
  userId?: null | number;
  userName?: null | string;
  userType?: null | number;
}

export interface LoginLogPageQueryParams {
  endDate?: null | string;
  ip?: null | string;
  pageNum: number;
  pageSize: number;
  startDate?: null | string;
  userName?: null | string;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildLoginLogPageQueryPayload(params: LoginLogPageQueryParams) {
  return {
    endDate: cleanText(params.endDate) || undefined,
    ip: cleanText(params.ip) || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    startDate: cleanText(params.startDate) || undefined,
    userName: cleanText(params.userName) || undefined,
  };
}

export async function queryLoginLogPage(params: LoginLogPageQueryParams) {
  return requestClient.post<PageResult<LoginLogRecord>>(
    '/admin/v1/platform/audit/login-logs/query',
    buildLoginLogPageQueryPayload(params),
  );
}
