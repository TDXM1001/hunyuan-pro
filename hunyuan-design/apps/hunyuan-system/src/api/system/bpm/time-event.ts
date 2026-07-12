import type { PageResult } from '#/api/system/organization';

import { requestClient } from '#/api/request';

export interface BpmTimeEventRecord {
  completedAt?: null | string;
  eventKey: string;
  eventKind: string;
  eventStatus: string;
  instanceId: number;
  lastError?: null | string;
  nodeKey: string;
  scheduledAt: string;
  taskId?: null | number;
  timeEventId: number;
  triggerCount: number;
  triggeredAt?: null | string;
}

export interface BpmExternalWaitRecord {
  callbackPayloadSnapshotJson?: null | string;
  cancelledAt?: null | string;
  connectorKey: string;
  connectorVersion: number;
  correlationKey: string;
  createTime: string;
  externalWaitId: number;
  instanceId: number;
  lastError?: null | string;
  nodeKey: string;
  operationKey: string;
  resumedAt?: null | string;
  timeoutAt: string;
  waitStatus: string;
}

export interface BpmConnectorDefinitionRecord {
  allowedOperationsJson: string;
  baseEndpointRef: string;
  circuitPolicyJson?: null | string;
  connectorDefinitionId?: number;
  connectorKey: string;
  connectorName: string;
  connectorVersion: number;
  credentialRef?: null | string;
  enabledState: string;
  requestSchemaJson: string;
  responseSchemaJson: string;
  retryPolicyJson: string;
  timeoutMillis: number;
}

export async function queryBpmTimeEventPage(params: Record<string, unknown>) {
  return requestClient.post<PageResult<BpmTimeEventRecord>>('/bpm/time-event/query', params);
}

export async function retryBpmTimeEvent(timeEventId: number) {
  return requestClient.post(`/bpm/time-event/retry/${timeEventId}`);
}

export async function queryBpmExternalWaitPage(params: Record<string, unknown>) {
  return requestClient.post<PageResult<BpmExternalWaitRecord>>('/bpm/external-wait/query', params);
}

export async function retryBpmExternalWait(externalWaitId: number) {
  return requestClient.post(`/bpm/external-wait/retry/${externalWaitId}`);
}

export async function cancelBpmExternalWait(externalWaitId: number) {
  return requestClient.post(`/bpm/external-wait/cancel/${externalWaitId}`);
}

export async function queryBpmConnectorPage(params: Record<string, unknown>) {
  return requestClient.post<PageResult<BpmConnectorDefinitionRecord>>('/bpm/connector/query', params);
}

export async function saveBpmConnector(params: BpmConnectorDefinitionRecord) {
  return requestClient.post<number>('/bpm/connector/save', params);
}
