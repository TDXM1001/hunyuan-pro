import { requestClient } from '#/api/request';

export interface PageResult<T> {
  emptyFlag?: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface JobRecord {
  createTime?: null | string;
  enabledFlag: boolean;
  jobClass: string;
  jobId: number;
  jobName: string;
  lastExecuteLogId?: null | number;
  lastExecuteTime?: null | string;
  nextJobExecuteTimeList?: null | string[];
  param?: null | string;
  remark?: null | string;
  sort: number;
  triggerType: string;
  triggerValue: string;
  updateName?: null | string;
  updateTime?: null | string;
}

export interface JobLogRecord {
  createName?: null | string;
  createTime?: null | string;
  executeEndTime?: null | string;
  executeResult?: null | string;
  executeStartTime?: null | string;
  executeTimeMillis?: null | number;
  ip?: null | string;
  jobId: number;
  jobName?: null | string;
  logId: number;
  param?: null | string;
  processId?: null | string;
  programPath?: null | string;
  successFlag?: null | boolean;
}

export interface JobPageQueryParams {
  deletedFlag?: boolean;
  enabledFlag?: boolean;
  pageNum: number;
  pageSize: number;
  searchWord?: null | string;
  triggerType?: null | string;
}

export interface JobMutationFormModel {
  enabledFlag: boolean;
  jobClass: string;
  jobId?: number;
  jobName: string;
  param?: null | string;
  remark?: null | string;
  sort: number;
  triggerType: string;
  triggerValue: string;
}

export interface JobEnabledFormModel {
  enabledFlag: boolean;
  jobId: number;
}

export interface JobExecuteFormModel {
  jobId: number;
  param?: null | string;
}

export interface JobLogQueryParams {
  endTime?: null | string;
  jobId?: null | number;
  pageNum: number;
  pageSize: number;
  searchWord?: null | string;
  startTime?: null | string;
  successFlag?: boolean;
}

function cleanText(value?: null | string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : '';
}

export function buildJobPageQueryPayload(params: JobPageQueryParams) {
  return {
    deletedFlag: params.deletedFlag,
    enabledFlag: params.enabledFlag,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    searchWord: cleanText(params.searchWord) || undefined,
    triggerType: cleanText(params.triggerType) || undefined,
  };
}

export function buildJobMutationPayload(params: JobMutationFormModel) {
  return {
    enabledFlag: params.enabledFlag,
    jobClass: cleanText(params.jobClass),
    jobId: params.jobId,
    jobName: cleanText(params.jobName),
    param: cleanText(params.param) || undefined,
    remark: cleanText(params.remark) || undefined,
    sort: params.sort,
    triggerType: cleanText(params.triggerType),
    triggerValue: cleanText(params.triggerValue),
  };
}

export function buildJobEnabledPayload(params: JobEnabledFormModel) {
  return {
    enabledFlag: params.enabledFlag,
    jobId: params.jobId,
  };
}

export function buildJobExecutePayload(params: JobExecuteFormModel) {
  return {
    jobId: params.jobId,
    param: cleanText(params.param) || undefined,
  };
}

export function buildJobLogQueryPayload(params: JobLogQueryParams) {
  return {
    endTime: cleanText(params.endTime) || undefined,
    jobId: params.jobId,
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    searchWord: cleanText(params.searchWord) || undefined,
    startTime: cleanText(params.startTime) || undefined,
    successFlag: params.successFlag,
  };
}

export function buildJobDeletePath(jobId: number) {
  return `/support/job/delete?jobId=${jobId}`;
}

export async function queryJobPage(params: JobPageQueryParams) {
  return requestClient.post<PageResult<JobRecord>>(
    '/support/job/query',
    buildJobPageQueryPayload(params),
  );
}

export async function addJob(params: JobMutationFormModel) {
  return requestClient.post<string>(
    '/support/job/add',
    buildJobMutationPayload(params),
  );
}

export async function updateJob(params: JobMutationFormModel) {
  return requestClient.post<string>(
    '/support/job/update',
    buildJobMutationPayload(params),
  );
}

export async function updateJobEnabled(params: JobEnabledFormModel) {
  return requestClient.post<string>(
    '/support/job/update/enabled',
    buildJobEnabledPayload(params),
  );
}

export async function executeJob(params: JobExecuteFormModel) {
  return requestClient.post<string>(
    '/support/job/execute',
    buildJobExecutePayload(params),
  );
}

export async function deleteJob(jobId: number) {
  return requestClient.get<string>(buildJobDeletePath(jobId));
}

export async function queryJobLogs(params: JobLogQueryParams) {
  return requestClient.post<PageResult<JobLogRecord>>(
    '/support/job/log/query',
    buildJobLogQueryPayload(params),
  );
}
