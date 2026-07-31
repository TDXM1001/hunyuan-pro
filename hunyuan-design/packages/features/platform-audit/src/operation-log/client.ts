import type { RequestClient } from '@vben/request';

/** 操作日志查询契约：记录后台用户执行了什么请求以及请求是否成功。 */
export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface OperateLogRecord {
  content?: null | string;
  createTime?: null | string;
  failReason?: null | string;
  ip?: null | string;
  ipRegion?: null | string;
  method?: null | string;
  module?: null | string;
  operateLogId: number;
  operateUserId?: null | number;
  operateUserName?: null | string;
  operateUserType?: null | number;
  param?: null | string;
  response?: null | string;
  successFlag?: null | boolean;
  updateTime?: null | string;
  url?: null | string;
  userAgent?: null | string;
}

export interface OperateLogPageQueryParams {
  endDate?: null | string;
  keywords?: null | string;
  pageNum: number;
  pageSize: number;
  requestKeywords?: null | string;
  startDate?: null | string;
  successFlag?: null | boolean;
  userName?: null | string;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

// 操作日志详情沿用列表查询中的稳定主键，页面不应自行拼接旧 support 路径。
export function buildOperateLogPageQueryPayload(params: OperateLogPageQueryParams) {
  return {
    endDate: cleanText(params.endDate) || undefined,
    keywords: cleanText(params.keywords) || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    requestKeywords: cleanText(params.requestKeywords) || undefined,
    startDate: cleanText(params.startDate) || undefined,
    successFlag: params.successFlag,
    userName: cleanText(params.userName) || undefined,
  };
}

export function buildOperateLogDetailPath(operateLogId: number) {
  // 详情接口只需要日志主键，页面不能用列表行里的 param/response 代替后端详情。
  return `/admin/v1/platform/audit/operation-logs/${operateLogId}`;
}

export async function queryOperateLogPage(
  requestClient: RequestClient,
  params: OperateLogPageQueryParams,
) {
  // 查询操作日志列表，返回结果可能包含脱敏后的请求地址、参数和执行结果。
  return requestClient.post<PageResult<OperateLogRecord>>(
    '/admin/v1/platform/audit/operation-logs/query',
    buildOperateLogPageQueryPayload(params),
  );
}

export async function queryOperateLogDetail(
  requestClient: RequestClient,
  operateLogId: number,
) {
  // 按主键获取单条详情，敏感内容由后端按当前用户权限决定是否返回。
  return requestClient.get<OperateLogRecord>(buildOperateLogDetailPath(operateLogId));
}
