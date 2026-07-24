import { requestClient } from '#/api/request';

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
  return `/admin/v1/platform/audit/operation-logs/${operateLogId}`;
}

export async function queryOperateLogPage(params: OperateLogPageQueryParams) {
  return requestClient.post<PageResult<OperateLogRecord>>(
    '/admin/v1/platform/audit/operation-logs/query',
    buildOperateLogPageQueryPayload(params),
  );
}

export async function queryOperateLogDetail(operateLogId: number) {
  return requestClient.get<OperateLogRecord>(buildOperateLogDetailPath(operateLogId));
}
