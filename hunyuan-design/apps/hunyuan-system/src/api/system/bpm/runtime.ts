import type { PageResult } from '#/api/system/organization';

import { requestClient } from '#/api/request';

export interface BpmInstanceRecord {
  finishedAt?: null | string;
  instanceId: number;
  instanceNo: string;
  resultState?: null | number;
  runState?: null | number;
  startEmployeeNameSnapshot?: null | string;
  startedAt?: null | string;
  title: string;
}

export interface BpmTaskActionLogRecord {
  actionAt?: null | string;
  actionLogId: number;
  actionType: string;
  actorEmployeeId?: null | number;
  actorNameSnapshot?: null | string;
  commentText?: null | string;
  definitionNodeId?: null | number;
  fromAssigneeEmployeeId?: null | number;
  taskId?: null | number;
  toAssigneeEmployeeId?: null | number;
}

export interface BpmInstanceDetailRecord extends BpmInstanceRecord {
  actionLogs: BpmTaskActionLogRecord[];
  currentFormDataSnapshotJson?: null | string;
  currentNodeSummaryJson?: null | string;
  startDepartmentNameSnapshot?: null | string;
  summary?: null | string;
}

export interface BpmTaskRecord {
  assignedAt?: null | string;
  assigneeNameSnapshot?: null | string;
  completedAt?: null | string;
  instanceId: number;
  instanceNo: string;
  instanceTitle: string;
  taskId: number;
  taskName: string;
  taskResult?: null | number;
  taskState?: null | number;
}

export interface BpmStartableDefinitionRecord {
  categoryNameSnapshot?: null | string;
  definitionId: number;
  definitionKey: string;
  definitionName: string;
  definitionVersion: number;
  formNameSnapshot?: null | string;
}

export interface BpmInstancePageQueryParams {
  instanceNo?: string;
  pageNum: number;
  pageSize: number;
  runState?: null | number;
  title?: string;
}

export interface BpmTaskPageQueryParams {
  instanceNo?: string;
  instanceTitle?: string;
  pageNum: number;
  pageSize: number;
  taskState?: null | number;
}

export interface BpmInstanceStartForm {
  businessId?: null | number;
  businessKey?: null | string;
  businessType?: null | string;
  definitionId: number;
  formDataJson: string;
  summary?: null | string;
  title?: null | string;
}

export interface BpmTaskApproveForm {
  commentText?: null | string;
  taskId: number;
}

export interface BpmTaskRejectForm {
  commentText?: null | string;
  taskId: number;
}

export interface BpmTaskReturnForm {
  commentText?: null | string;
  taskId: number;
}

export interface BpmTaskTransferForm {
  commentText?: null | string;
  taskId: number;
  toEmployeeId: number;
}

export async function queryBpmInstancePage(params: BpmInstancePageQueryParams) {
  return requestClient.post<PageResult<BpmInstanceRecord>>('/bpm/instance/query', {
    instanceNo: params.instanceNo?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    runState: params.runState ?? undefined,
    title: params.title?.trim() || undefined,
  });
}

export async function queryBpmTaskPage(params: BpmTaskPageQueryParams) {
  return requestClient.post<PageResult<BpmTaskRecord>>('/bpm/task/query', {
    instanceNo: params.instanceNo?.trim() || undefined,
    instanceTitle: params.instanceTitle?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    taskState: params.taskState ?? undefined,
  });
}

export async function queryBpmStartableDefinitions() {
  return requestClient.get<BpmStartableDefinitionRecord[]>('/app/bpm/startable');
}

export async function startBpmInstance(params: BpmInstanceStartForm) {
  return requestClient.post<number>('/app/bpm/start', {
    ...params,
    businessKey: params.businessKey?.trim() || '',
    businessType: params.businessType?.trim() || '',
    formDataJson: params.formDataJson.trim(),
    summary: params.summary?.trim() || '',
    title: params.title?.trim() || '',
  });
}

export async function queryMyBpmInstancePage(params: BpmInstancePageQueryParams) {
  return requestClient.post<PageResult<BpmInstanceRecord>>(
    '/app/bpm/my-instance',
    {
      instanceNo: params.instanceNo?.trim() || undefined,
      pageNum: params.pageNum,
      pageSize: params.pageSize,
      runState: params.runState ?? undefined,
      title: params.title?.trim() || undefined,
    },
  );
}

export async function getBpmInstanceDetail(instanceId: number) {
  return requestClient.get<BpmInstanceDetailRecord>(
    `/app/bpm/instance/detail/${instanceId}`,
  );
}

export async function queryMyBpmTodoPage(params: BpmTaskPageQueryParams) {
  return requestClient.post<PageResult<BpmTaskRecord>>('/app/bpm/my-todo', {
    instanceNo: params.instanceNo?.trim() || undefined,
    instanceTitle: params.instanceTitle?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    taskState: params.taskState ?? undefined,
  });
}

export async function queryMyBpmDonePage(params: BpmTaskPageQueryParams) {
  return requestClient.post<PageResult<BpmTaskRecord>>('/app/bpm/my-done', {
    instanceNo: params.instanceNo?.trim() || undefined,
    instanceTitle: params.instanceTitle?.trim() || undefined,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    taskState: params.taskState ?? undefined,
  });
}

export async function approveBpmTask(params: BpmTaskApproveForm) {
  return requestClient.post<string>('/app/bpm/task/approve', {
    commentText: params.commentText?.trim() || '',
    taskId: params.taskId,
  });
}

export async function rejectBpmTask(params: BpmTaskRejectForm) {
  return requestClient.post<string>('/app/bpm/task/reject', {
    commentText: params.commentText?.trim() || '',
    taskId: params.taskId,
  });
}

export async function returnBpmTaskToInitiator(params: BpmTaskReturnForm) {
  return requestClient.post<string>('/app/bpm/task/returnToInitiator', {
    commentText: params.commentText?.trim() || '',
    taskId: params.taskId,
  });
}

export async function transferBpmTask(params: BpmTaskTransferForm) {
  return requestClient.post<string>('/app/bpm/task/transfer', {
    commentText: params.commentText?.trim() || '',
    taskId: params.taskId,
    toEmployeeId: params.toEmployeeId,
  });
}
