import type { PageResult } from '#/api/system/organization';

import { requestClient } from '#/api/request';

export type BpmOperationsActionType =
  | 'ARCHIVE'
  | 'COMPENSATE'
  | 'RETRY'
  | 'TERMINATE';

export interface BpmOperationsCaseVO {
  assigneeEmployeeId?: null | number;
  businessId?: null | number;
  businessKey?: null | string;
  businessType?: null | string;
  caseCode: string;
  caseStatus: string;
  compensableFlag: boolean;
  definitionNodeId?: null | string;
  eventId?: null | string;
  failureCode?: null | string;
  failureReason?: null | string;
  graphDefinitionVersionId?: null | number;
  highRiskFlag: boolean;
  instanceId?: null | number;
  lastActionAt?: null | string;
  legalHoldFlag: boolean;
  nodeName?: null | string;
  openedAt: string;
  operationsCaseId: number;
  organizationId?: null | number;
  resolvedAt?: null | string;
  retentionUntil?: null | string;
  retryableFlag: boolean;
  severity: string;
  slaLevel: string;
  sourceId: number;
  sourceType: string;
}

export interface BpmOperationsCasePageQuery {
  assigneeEmployeeId?: null | number;
  businessKey?: string;
  caseStatus?: string;
  definitionNodeId?: string;
  eventId?: string;
  failureCode?: string;
  graphDefinitionVersionId?: null | number;
  organizationId?: null | number;
  pageNum: number;
  pageSize: number;
  slaLevel?: string;
}

export interface BpmOperationsActionParams {
  actionType: BpmOperationsActionType;
  idempotencyKey: string;
  reason: string;
}

export interface BpmOperationsActionResultVO {
  actionStatus: string;
  message: string;
  operationsActionLogId?: null | number;
}

export interface BpmOperationsActionLogVO {
  actionAt: string;
  actionStatus: string;
  actionType: BpmOperationsActionType;
  actorEmployeeId?: null | number;
  failureReason?: null | string;
  operationsActionLogId: number;
  reason: string;
}

export interface BpmOperationsCaseDetailVO extends BpmOperationsCaseVO {
  actionLogs: BpmOperationsActionLogVO[];
}

export interface BpmOperationsMetricQuery {
  definitionNodeId?: string;
  graphDefinitionVersionId?: null | number;
  organizationId?: null | number;
}

export interface BpmOperationsMetricVO {
  averageHandlingMinutes: number;
  compensableCount: number;
  failureCode?: null | string;
  graphDefinitionVersionId?: null | number;
  nodeId?: null | string;
  metricDate?: null | string;
  openCount: number;
  organizationId?: null | number;
  retryableCount: number;
  slaBreachedCount: number;
  totalCount: number;
}

export interface BpmOperationsRetentionDecisionVO {
  allowed: boolean;
  reason: string;
}

function cleanText(value?: null | string) {
  return value?.trim() || undefined;
}

export async function queryBpmOperationsCasePage(data: BpmOperationsCasePageQuery) {
  return requestClient.post<PageResult<BpmOperationsCaseVO>>(
    '/bpm/operations/case/query',
    {
      ...data,
      businessKey: cleanText(data.businessKey),
      caseStatus: cleanText(data.caseStatus),
      definitionNodeId: cleanText(data.definitionNodeId),
      eventId: cleanText(data.eventId),
      failureCode: cleanText(data.failureCode),
      slaLevel: cleanText(data.slaLevel),
    },
  );
}

export async function getBpmOperationsCaseDetail(operationsCaseId: number) {
  return requestClient.post<BpmOperationsCaseDetailVO>(
    `/bpm/operations/case/detail/${operationsCaseId}`,
  );
}

export async function exportBpmOperationsCases(data: BpmOperationsCasePageQuery) {
  return requestClient.post<BpmOperationsCaseVO[]>('/bpm/operations/case/export', {
    ...data,
    businessKey: cleanText(data.businessKey),
    caseStatus: cleanText(data.caseStatus),
    eventId: cleanText(data.eventId),
    failureCode: cleanText(data.failureCode),
    slaLevel: cleanText(data.slaLevel),
  });
}

export async function executeBpmOperationsAction(
  operationsCaseId: number,
  data: BpmOperationsActionParams,
) {
  return requestClient.post<BpmOperationsActionResultVO>(
    `/bpm/operations/action/${operationsCaseId}`,
    {
      actionType: data.actionType,
      idempotencyKey: data.idempotencyKey.trim(),
      reason: data.reason.trim(),
    },
  );
}

export async function queryBpmOperationsMetrics(data: BpmOperationsMetricQuery = {}) {
  return requestClient.post<BpmOperationsMetricVO[]>(
    '/bpm/operations/metrics/query',
    {
      ...data,
      definitionNodeId: cleanText(data.definitionNodeId),
    },
  );
}

export async function evaluateBpmOperationsRetention(operationsCaseId: number) {
  return requestClient.post<BpmOperationsRetentionDecisionVO>(
    '/bpm/operations/retention/evaluate',
    { operationsCaseId },
  );
}
