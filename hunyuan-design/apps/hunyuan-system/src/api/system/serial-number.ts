import { requestClient } from '#/api/request';

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
  return {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    serialNumberId: params.serialNumberId,
  };
}

export function buildSerialNumberGeneratePayload(
  params: SerialNumberGenerateParams,
) {
  return {
    count: params.count,
    serialNumberId: params.serialNumberId,
  };
}

export async function querySerialNumberList() {
  return requestClient.get<SerialNumberDefinition[]>('/support/serialNumber/all');
}

export async function querySerialNumberRecords(
  params: SerialNumberRecordQueryParams,
) {
  return requestClient.post<PageResult<SerialNumberRecord>>(
    '/support/serialNumber/queryRecord',
    buildSerialNumberRecordQueryPayload(params),
  );
}

export async function generateSerialNumbers(params: SerialNumberGenerateParams) {
  return requestClient.post<string[]>(
    '/support/serialNumber/generate',
    buildSerialNumberGeneratePayload(params),
  );
}
