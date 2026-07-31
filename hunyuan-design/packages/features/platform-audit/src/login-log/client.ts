import type { RequestClient } from '@vben/request';

/** 登录日志查询契约：记录用户何时、从哪里、以什么结果登录系统。 */
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

// 审计查询的日期和文本筛选统一转换为空值省略，避免空字符串改变后端查询语义。
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

export async function queryLoginLogPage(
  requestClient: RequestClient,
  params: LoginLogPageQueryParams,
) {
  // 登录日志只读查询；IP、用户名和日期为空时由 payload builder 处理为未设置条件。
  return requestClient.post<PageResult<LoginLogRecord>>(
    '/admin/v1/platform/audit/login-logs/query',
    buildLoginLogPageQueryPayload(params),
  );
}
