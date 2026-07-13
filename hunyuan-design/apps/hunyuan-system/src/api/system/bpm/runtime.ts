import type { PageResult } from '#/api/system/organization';
import type {
  BpmCallbackRecordVO,
  BpmCommandRecordVO,
} from '#/api/system/bpm/integration';

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

export type BpmApprovalGroupState =
  | 'APPROVED'
  | 'CANCELLED'
  | 'PENDING'
  | 'REJECTED'
  | 'RETURNED';

export type BpmApprovalMode = 'parallelAll' | 'sequential';

export type BpmFieldPermissionMode = 'EDITABLE' | 'HIDDEN' | 'READONLY';

export interface BpmFieldPermission {
  fieldKey: string;
  permission: BpmFieldPermissionMode;
  required: boolean;
}

export interface BpmTaskFormContext {
  dataVersion: number;
  formDataJson: string;
  formSchemaJson: string;
  permissions: BpmFieldPermission[];
}

export interface BpmApprovalGroupSummaryRecord {
  approvalGroupId: number;
  approvalGroupKey: string;
  approvalGroupName: string;
  approvalMode: BpmApprovalMode;
  approvedMemberCount: number;
  groupState: BpmApprovalGroupState;
  processedMemberCount: number;
  rejectedMemberCount: number;
  totalMemberCount: number;
}

export interface BpmApprovalGroupMemberRecord {
  assignedAt?: null | string;
  assigneeDepartmentNameSnapshot?: null | string;
  assigneeEmployeeId?: null | number;
  assigneeNameSnapshot?: null | string;
  cancelledAt?: null | string;
  completedAt?: null | string;
  lastAction?: BpmTaskActionLogRecord | null;
  memberIndex?: null | number;
  memberTotal?: null | number;
  taskId: number;
  taskName: string;
  taskResult?: null | number;
  taskState?: null | number;
}

export interface BpmApprovalGroupDetailRecord
  extends BpmApprovalGroupSummaryRecord {
  closeReason?: null | string;
  closedAt?: null | string;
  members: BpmApprovalGroupMemberRecord[];
}

export interface BpmInstanceDetailRecord extends BpmInstanceRecord {
  actionLogs: BpmTaskActionLogRecord[];
  approvalGroups: BpmApprovalGroupDetailRecord[];
  currentFormDataSnapshotJson?: null | string;
  currentNodeSummaryJson?: null | string;
  currentTasks?: BpmTaskRecord[];
  startDepartmentNameSnapshot?: null | string;
  summary?: null | string;
}

export interface BpmNotificationRecordVO {
  channel: string;
  contentSnapshot?: null | string;
  createTime?: null | string;
  definitionId?: null | number;
  definitionNodeId?: null | number;
  eventKey: string;
  failReason?: null | string;
  instanceId: number;
  notificationRecordId: number;
  receiverEmployeeId?: null | number;
  receiverSnapshotJson?: null | string;
  requestPayloadJson?: null | string;
  responseSnapshotJson?: null | string;
  sendStatus?: null | number;
  sentAt?: null | string;
  taskId?: null | number;
  templateCode?: null | string;
  title?: null | string;
  updateTime?: null | string;
}

export interface BpmApprovalStageMemberTraceRecord {
  actionResult?: null | string;
  activatedAt?: null | string;
  approvalStageMemberId: number;
  cancelledAt?: null | string;
  changeReason?: null | string;
  completedAt?: null | string;
  currentEmployeeId: number;
  memberOrder: number;
  memberState: string;
  sourceEmployeeId: number;
  sourceEmployeeNameSnapshot?: null | string;
  currentEmployeeNameSnapshot?: null | string;
}

export interface BpmApprovalStageTraceRecord {
  approvalPolicyVersionId: number;
  approvalStageId: number;
  approvedMemberCount: number;
  authoredNodeId: string;
  candidatePolicyVersionId: number;
  closedAt?: null | string;
  completionMode: string;
  effectiveMemberCount: number;
  generation: number;
  members: BpmApprovalStageMemberTraceRecord[];
  openedAt?: null | string;
  processedMemberCount: number;
  ratioPercent: number;
  requiredApprovalCount: number;
  stageInvocationId: string;
  stageState: string;
  terminalReason?: null | string;
}

export interface BpmInstanceTraceRecord {
  actionLogs: BpmTaskActionLogRecord[];
  approvalGroups: BpmApprovalGroupDetailRecord[];
  approvalStages: BpmApprovalStageTraceRecord[];
  callbackRecords: BpmCallbackRecordVO[];
  commandRecords: BpmCommandRecordVO[];
  currentTasks: BpmTaskRecord[];
  formDataChanges: BpmFormDataChangeRecord[];
  instance: BpmInstanceDetailRecord;
  notificationRecords: BpmNotificationRecordVO[];
  timeEvents: import('./time-event').BpmTimeEventRecord[];
  externalWaits: import('./time-event').BpmExternalWaitRecord[];
  processGraph?: BpmRuntimeGraphRecord;
  routeDecisions?: BpmRouteDecisionRecord[];
}

export interface BpmRuntimeGraphNodeRecord {
  branchPath: string[];
  definitionNodeId: number;
  nodeKey: string;
  nodeName: string;
  nodeType: string;
  sortOrder?: null | number;
  state: 'ACTIVE' | 'CANCELLED' | 'COMPLETED' | 'NOT_ENTERED' | 'SKIPPED';
}

export interface BpmRouteDecisionRecord {
  defaultBranchUsed?: boolean;
  definitionId: number;
  definitionNodeId: number;
  evaluatedAt?: null | string;
  evaluationStatus?: null | string;
  inputFormDataVersion?: null | number;
  instanceId: number;
  matchedBranchKeys: string[];
  reasonSnapshotJson?: null | string;
  routeDecisionId: number;
  routeNodeKey: string;
}

export interface BpmRuntimeGraphRecord {
  definitionId: number;
  instanceId: number;
  nodes: BpmRuntimeGraphNodeRecord[];
  routeDecisions: BpmRouteDecisionRecord[];
}

export interface BpmFormDataChangeRecord {
  actorEmployeeId?: null | number;
  actorNameSnapshot?: null | string;
  afterValuesJson: string;
  afterVersion: number;
  beforeValuesJson: string;
  beforeVersion: number;
  changeId: number;
  changeSource: 'INSTANCE_RESUBMITTED' | 'INSTANCE_STARTED' | 'TASK_APPROVED';
  changedFieldsJson: string;
  createTime?: null | string;
  definitionNodeId?: null | number;
  instanceId: number;
  nodeKeySnapshot?: null | string;
  taskId?: null | number;
}

export interface BpmInstanceCopyRecord {
  copyId: number;
  copyType: string;
  instanceId: number;
  instanceNo: string;
  readAt?: null | string;
  readState?: null | number;
  reasonSnapshot?: null | string;
  resultState?: null | number;
  runState?: null | number;
  sentAt?: null | string;
  sourceNodeName?: null | string;
  startEmployeeNameSnapshot?: null | string;
  targetNameSnapshot?: null | string;
  title: string;
}

export interface BpmTaskRecord {
  availableActions?: BpmTaskAction[];
  approvalGroup?: BpmApprovalGroupSummaryRecord | null;
  assignedAt?: null | string;
  assigneeDepartmentNameSnapshot?: null | string;
  assigneeNameSnapshot?: null | string;
  completedAt?: null | string;
  instanceId: number;
  instanceNo: string;
  instanceTitle: string;
  startEmployeeNameSnapshot?: null | string;
  taskId: number;
  taskKind?: 'APPROVAL' | 'HANDLE';
  taskKey?: null | string;
  taskName: string;
  dueAt?: null | string;
  runtimeAssignmentSnapshotJson?: null | string;
  taskResult?: null | number;
  taskState?: null | number;
}

export type BpmTaskAction =
  | 'ADD_SIGN'
  | 'APPROVE'
  | 'COMPLETE'
  | 'DELEGATE'
  | 'REDUCE_SIGN'
  | 'REJECT'
  | 'RETURN'
  | 'TRANSFER';

export interface BpmTaskDetailRecord extends BpmTaskRecord {
  actionLogs: BpmTaskActionLogRecord[];
  approvalGroup?: BpmApprovalGroupDetailRecord | null;
  formContext?: BpmTaskFormContext | null;
}

export interface BpmStartableDefinitionRecord {
  categoryNameSnapshot?: null | string;
  definitionId?: null | number;
  definitionSource?: 'GRAPH' | 'LEGACY';
  definitionKey: string;
  definitionName: string;
  definitionVersion: number;
  formNameSnapshot?: null | string;
  graphDefinitionVersionId?: null | number;
}

export interface BpmRuntimeStartDraftRecord {
  definitionId?: null | number;
  definitionSource?: 'GRAPH' | 'LEGACY';
  definitionName: string;
  formDataJson: string;
  formDataVersion?: null | number;
  formNameSnapshot?: null | string;
  formSchemaSnapshotJson: string;
  graphDefinitionVersionId?: null | number;
  sourceInstanceId?: null | number;
  summary?: null | string;
  title: string;
}

export interface BpmInstancePageQueryParams {
  instanceNo?: string;
  pageNum: number;
  pageSize: number;
  runState?: null | number;
  title?: string;
}

export interface BpmInstanceCopyPageQueryParams {
  instanceNo?: string;
  pageNum: number;
  pageSize: number;
  readState?: null | number;
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
  definitionId?: null | number;
  formDataJson: string;
  graphDefinitionVersionId?: null | number;
  summary?: null | string;
  title?: null | string;
}

export interface BpmInstanceCancelForm {
  cancelReason?: null | string;
  instanceId: number;
}

export interface BpmInstanceResubmitForm {
  formDataJson: string;
  formDataVersion: number;
  instanceId: number;
  summary?: null | string;
  title?: null | string;
}

export interface BpmTaskApproveForm {
  commentText?: null | string;
  copyEmployeeIds?: number[];
  formDataPatchJson?: null | string;
  formDataVersion?: null | number;
  requestId?: string;
  taskId: number;
}

export interface BpmTaskRejectForm {
  commentText?: null | string;
  copyEmployeeIds?: number[];
  requestId?: string;
  taskId: number;
}

export interface BpmTaskCompleteForm {
  commentText?: null | string;
  taskId: number;
}

export interface BpmTaskReturnForm {
  commentText?: null | string;
  copyEmployeeIds?: number[];
  requestId?: string;
  taskId: number;
}

export interface BpmTaskTransferForm {
  commentText?: null | string;
  taskId: number;
  toEmployeeId: number;
}

export interface BpmTaskDelegateForm {
  reason?: null | string;
  targetEmployeeId: number;
  taskId: number;
}

export interface BpmTaskAddSignForm {
  reason?: null | string;
  targetEmployeeId: number;
  taskId: number;
}

export interface BpmTaskReduceSignForm {
  reason?: null | string;
  taskId: number;
}

export interface BpmTaskRecallForm {
  reason?: null | string;
  taskId: number;
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

export async function getBpmAdminInstanceDetail(instanceId: number) {
  return requestClient.get<BpmInstanceDetailRecord>(
    `/bpm/instance/detail/${instanceId}`,
  );
}

export async function getBpmAdminInstanceTrace(instanceId: number) {
  return requestClient.get<BpmInstanceTraceRecord>(
    `/bpm/instance/trace/${instanceId}`,
  );
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

export async function getBpmTaskDetail(taskId: number) {
  return requestClient.get<BpmTaskDetailRecord>(
    `/app/bpm/task/detail/${taskId}`,
  );
}

export async function queryBpmStartableDefinitions() {
  return requestClient.get<BpmStartableDefinitionRecord[]>('/app/bpm/startable');
}

export async function getBpmStartDraft(definitionId: number) {
  return requestClient.get<BpmRuntimeStartDraftRecord>(
    `/app/bpm/start-draft/${definitionId}`,
  );
}

export async function getBpmGraphStartDraft(graphDefinitionVersionId: number) {
  return requestClient.get<BpmRuntimeStartDraftRecord>(
    `/app/bpm/graph-start-draft/${graphDefinitionVersionId}`,
  );
}

export async function getBpmResubmitDraft(instanceId: number) {
  return requestClient.get<BpmRuntimeStartDraftRecord>(
    `/app/bpm/resubmit-draft/${instanceId}`,
  );
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

export async function getBpmInstanceTrace(instanceId: number) {
  return requestClient.get<BpmInstanceTraceRecord>(
    `/app/bpm/instance/trace/${instanceId}`,
  );
}

export async function cancelMyBpmInstance(params: BpmInstanceCancelForm) {
  return requestClient.post<string>('/app/bpm/instance/cancel', {
    cancelReason: params.cancelReason?.trim() || '',
    instanceId: params.instanceId,
  });
}

export async function resubmitMyBpmInstance(params: BpmInstanceResubmitForm) {
  return requestClient.post<number>('/app/bpm/instance/resubmit', {
    formDataJson: params.formDataJson.trim(),
    formDataVersion: params.formDataVersion,
    instanceId: params.instanceId,
    summary: params.summary?.trim() || '',
    title: params.title?.trim() || '',
  });
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

export async function queryMyBpmCopyPage(
  params: BpmInstanceCopyPageQueryParams,
) {
  return requestClient.post<PageResult<BpmInstanceCopyRecord>>(
    '/app/bpm/my-copy',
    {
      instanceNo: params.instanceNo?.trim() || undefined,
      pageNum: params.pageNum,
      pageSize: params.pageSize,
      readState: params.readState ?? undefined,
      title: params.title?.trim() || undefined,
    },
  );
}

export async function markBpmCopyRead(copyId: number) {
  return requestClient.post<string>(`/app/bpm/copy/read/${copyId}`);
}

export async function approveBpmTask(params: BpmTaskApproveForm) {
  return requestClient.post<string>('/app/bpm/task/approve', {
    commentText: params.commentText?.trim() || '',
    copyEmployeeIds: params.copyEmployeeIds ?? [],
    formDataPatchJson: params.formDataPatchJson?.trim() || undefined,
    formDataVersion: params.formDataVersion ?? undefined,
    requestId: params.requestId ?? crypto.randomUUID(),
    taskId: params.taskId,
  });
}

export async function completeBpmTask(params: BpmTaskCompleteForm) {
  return requestClient.post<string>('/app/bpm/task/complete', {
    commentText: params.commentText?.trim() || '',
    taskId: params.taskId,
  });
}

export async function rejectBpmTask(params: BpmTaskRejectForm) {
  return requestClient.post<string>('/app/bpm/task/reject', {
    commentText: params.commentText?.trim() || '',
    copyEmployeeIds: params.copyEmployeeIds ?? [],
    requestId: params.requestId ?? crypto.randomUUID(),
    taskId: params.taskId,
  });
}

export async function returnBpmTaskToInitiator(params: BpmTaskReturnForm) {
  return requestClient.post<string>('/app/bpm/task/returnToInitiator', {
    commentText: params.commentText?.trim() || '',
    copyEmployeeIds: params.copyEmployeeIds ?? [],
    requestId: params.requestId ?? crypto.randomUUID(),
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

export async function delegateBpmTask(params: BpmTaskDelegateForm) {
  return requestClient.post<string>('/app/bpm/task/delegate', {
    reason: params.reason?.trim() || '',
    targetEmployeeId: params.targetEmployeeId,
    taskId: params.taskId,
  });
}

export async function addSignBpmTask(params: BpmTaskAddSignForm) {
  return requestClient.post<string>('/app/bpm/task/addSign', {
    reason: params.reason?.trim() || '',
    targetEmployeeId: params.targetEmployeeId,
    taskId: params.taskId,
  });
}

export async function reduceSignBpmTask(params: BpmTaskReduceSignForm) {
  return requestClient.post<string>('/app/bpm/task/reduceSign', {
    reason: params.reason?.trim() || '',
    taskId: params.taskId,
  });
}

export async function recallBpmTask(params: BpmTaskRecallForm) {
  return requestClient.post<string>('/app/bpm/task/recall', {
    reason: params.reason?.trim() || '',
    taskId: params.taskId,
  });
}
