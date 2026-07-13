import type { PageResult } from '#/api/system/organization';

import { requestClient } from '#/api/request';

export interface BpmCallbackRecordVO {
  businessId: number;
  businessType: string;
  callbackRecordId: number;
  callbackStatus: number;
  compensatedAt?: null | string;
  compensatedBy?: null | number;
  compensationReason?: null | string;
  createTime?: null | string;
  eventId: string;
  failureReason?: null | string;
  instanceId: number;
  nextRetryAt?: null | string;
  retryCount: number;
  updateTime?: null | string;
}

export interface BpmCommandRecordVO {
  businessId?: null | number;
  businessType?: null | string;
  commandKey: string;
  commandRecordId: number;
  commandStatus: number;
  commandType: string;
  createTime?: null | string;
  failureReason?: null | string;
  instanceId?: null | number;
  updateTime?: null | string;
}

export interface BpmCallbackRecordPageQueryParams {
  businessId?: null | number;
  businessType?: string;
  callbackStatus?: null | number;
  eventId?: string;
  instanceId?: null | number;
  pageNum: number;
  pageSize: number;
}

export interface BpmCallbackCompensateParams {
  reason: string;
}

export interface BpmCommandRecordPageQueryParams {
  businessId?: null | number;
  businessType?: string;
  commandKey?: string;
  commandStatus?: null | number;
  instanceId?: null | number;
  pageNum: number;
  pageSize: number;
}

export async function queryBpmCallbackRecordPage(
  data: BpmCallbackRecordPageQueryParams,
) {
  return requestClient.post<PageResult<BpmCallbackRecordVO>>(
    '/bpm/integration/callback/query',
    {
      businessId: data.businessId ?? undefined,
      businessType: data.businessType?.trim() || undefined,
      callbackStatus: data.callbackStatus ?? undefined,
      eventId: data.eventId?.trim() || undefined,
      instanceId: data.instanceId ?? undefined,
      pageNum: data.pageNum,
      pageSize: data.pageSize,
    },
  );
}

export async function retryBpmCallbackRecord(callbackRecordId: number) {
  return requestClient.post<string>(
    `/bpm/integration/callback/retry/${callbackRecordId}`,
  );
}

export async function compensateBpmCallbackRecord(
  callbackRecordId: number,
  data: BpmCallbackCompensateParams,
) {
  return requestClient.post<string>(
    `/bpm/integration/callback/compensate/${callbackRecordId}`,
    {
      reason: data.reason.trim(),
    },
  );
}

export async function queryBpmCommandRecordPage(
  data: BpmCommandRecordPageQueryParams,
) {
  return requestClient.post<PageResult<BpmCommandRecordVO>>(
    '/bpm/integration/command/query',
    {
      businessId: data.businessId ?? undefined,
      businessType: data.businessType?.trim() || undefined,
      commandKey: data.commandKey?.trim() || undefined,
      commandStatus: data.commandStatus ?? undefined,
      instanceId: data.instanceId ?? undefined,
      pageNum: data.pageNum,
      pageSize: data.pageSize,
    },
  );
}

export type M6ConfigKind = 'applications' | 'bindings' | 'mappings' | 'subscriptions' | 'systems';
export type M6ConfigRecord = Record<string, any>;
export async function listM6Configs(kind: M6ConfigKind) {
  return requestClient.get<M6ConfigRecord[]>(`/bpm/integration/config/${kind}`);
}
export async function saveM6Config(kind: M6ConfigKind, data: M6ConfigRecord) {
  return requestClient.post<number>(`/bpm/integration/config/${kind}`, data);
}
export async function publishM6Config(kind: 'bindings' | 'subscriptions' | 'systems', id: number) {
  return requestClient.post<number>(`/bpm/integration/config/${kind}/${id}/publish`);
}
