import type { RequestClient } from '@vben/request';

/** 序列号规则与生成记录的查询模型；号码分配本身只由后端负责。 */
export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface SerialNumberDefinition {
  businessName: string;
  createTime?: null | string;
  format: string;
  initNumber?: null | number;
  lastNumber?: null | number;
  lastTime?: null | string;
  remark?: null | string;
  ruleType?: null | string;
  serialNumberId: number;
  stepRandomRange?: null | number;
  updateTime?: null | string;
}

export interface SerialNumberRecord {
  count?: null | number;
  createTime?: null | string;
  lastNumber?: null | number;
  lastTime?: null | string;
  recordDate?: null | string;
  serialNumberId: number;
  updateTime?: null | string;
}

export interface SerialNumberRecordQueryParams {
  pageNum: number;
  pageSize: number;
  serialNumberId: number;
}

export interface SerialNumberGenerateParams {
  count: number;
  serialNumberId: number;
}

export function buildSerialNumberRecordQueryPayload(
  params: SerialNumberRecordQueryParams,
) {
  // 记录查询必须带 serialNumberId，否则不同业务规则的流水会混在一起。
  return {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    serialNumberId: params.serialNumberId,
  };
}

export function buildSerialNumberGeneratePayload(
  params: SerialNumberGenerateParams,
) {
  // 生成请求只携带数量和规则 ID，具体序号分配由后端保证原子性。
  return {
    count: params.count,
    serialNumberId: params.serialNumberId,
  };
}

export async function querySerialNumberList(requestClient: RequestClient) {
  // 获取系统中配置好的序列号规则，不在前端计算或预测下一个号码。
  return requestClient.get<SerialNumberDefinition[]>(
    '/admin/v1/platform/runtime/serial-numbers',
  );
}

export async function querySerialNumberRecords(
  requestClient: RequestClient,
  params: SerialNumberRecordQueryParams,
) {
  // 查询某条序列号规则的历史生成记录，并保留后端分页信息。
  return requestClient.post<PageResult<SerialNumberRecord>>(
    '/admin/v1/platform/runtime/serial-numbers/records/query',
    buildSerialNumberRecordQueryPayload(params),
  );
}

export async function generateSerialNumbers(requestClient: RequestClient, params: SerialNumberGenerateParams) {
  // 请求后端生成指定数量的序列号；并发安全和号码连续性由后端保证。
  return requestClient.post<string[]>(
    '/admin/v1/platform/runtime/serial-numbers/generate',
    buildSerialNumberGeneratePayload(params),
  );
}
