import type { RequestClient } from '@vben/request';

/**
 * 定时任务的请求与返回模型。
 * 任务配置、启停、手动执行、删除和执行日志是不同动作，分别使用不同的参数类型，避免误把一次性操作当成配置修改。
 */
const JOB_BASE_PATH = '/admin/v1/platform/runtime/jobs';

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
  /** 将任务列表的筛选条件转换成后端分页查询请求。 */
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
  // 调度任务的文本字段统一裁剪；可选参数为空时省略，避免覆盖后端默认值。
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
  // 启停操作只改变 enabledFlag，不把任务名称、类名等配置字段一起提交。
  return {
    enabledFlag: params.enabledFlag,
    jobId: params.jobId,
  };
}

export function buildJobExecutePayload(params: JobExecuteFormModel) {
  // 手动执行仍复用任务 ID，参数只作为本次执行输入，不修改任务配置。
  return {
    jobId: params.jobId,
    param: cleanText(params.param) || undefined,
  };
}

export function buildJobLogQueryPayload(params: JobLogQueryParams) {
  /** 将任务日志的时间、成功状态和关键词筛选转换成查询请求。 */
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
  // 删除路径只接收任务主键，调用方无需知道后端的完整 URL 前缀。
  return `${JOB_BASE_PATH}/${jobId}`;
}

export async function queryJobPage(requestClient: RequestClient, params: JobPageQueryParams) {
  // 查询任务列表，并保留服务端返回的分页总数。
  return requestClient.post<PageResult<JobRecord>>(
    `${JOB_BASE_PATH}/query`,
    buildJobPageQueryPayload(params),
  );
}

export async function addJob(requestClient: RequestClient, params: JobMutationFormModel) {
  // 新增任务由后端生成 jobId，页面只提交表单配置。
  return requestClient.post<string>(
    JOB_BASE_PATH,
    buildJobMutationPayload(params),
  );
}

export async function updateJob(requestClient: RequestClient, params: JobMutationFormModel) {
  // 更新沿用统一任务路径，jobId 从请求体中定位要修改的任务。
  return requestClient.put<string>(
    JOB_BASE_PATH,
    buildJobMutationPayload(params),
  );
}

export async function updateJobEnabled(requestClient: RequestClient, params: JobEnabledFormModel) {
  // 单独更新启用状态，避免启停按钮覆盖任务其他配置。
  return requestClient.put<string>(
    `${JOB_BASE_PATH}/enabled`,
    buildJobEnabledPayload(params),
  );
}

export async function executeJob(requestClient: RequestClient, params: JobExecuteFormModel) {
  // 手动执行是一次性动作，只影响本次运行，不改变保存的调度规则。
  return requestClient.post<string>(
    `${JOB_BASE_PATH}/execute`,
    buildJobExecutePayload(params),
  );
}

export async function deleteJob(requestClient: RequestClient, jobId: number) {
  // 删除任务是不可逆的管理动作，确认流程在页面层完成，客户端只负责调用接口。
  return requestClient.delete<string>(buildJobDeletePath(jobId));
}

export async function queryJobLogs(requestClient: RequestClient, params: JobLogQueryParams) {
  // 查询指定任务的执行记录，日志分页与任务列表分页互相独立。
  return requestClient.post<PageResult<JobLogRecord>>(
    `${JOB_BASE_PATH}/logs/query`,
    buildJobLogQueryPayload(params),
  );
}
