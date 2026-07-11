<script setup lang="ts">
import type { BpmTaskDetailRecord, BpmTaskRecord } from '#/api/system/bpm';
import type { EmployeeRecord } from '#/api/system/organization';
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, onMounted, reactive, ref } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  useTableColumns,
} from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElCard,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElEmpty,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElSelect,
  ElTag,
  ElTimeline,
  ElTimelineItem,
} from 'element-plus';

import {
  addSignBpmTask,
  approveBpmTask,
  completeBpmTask,
  delegateBpmTask,
  getBpmTaskDetail,
  queryMyBpmTodoPage,
  recallBpmTask,
  reduceSignBpmTask,
  rejectBpmTask,
  returnBpmTaskToInitiator,
  transferBpmTask,
} from '#/api/system/bpm';
import { queryEmployeePage } from '#/api/system/organization';

import BpmApprovalGroupPanel from './components/bpm-approval-group-panel.vue';
import BpmTaskFormWorkbench from './components/bpm-task-form-workbench.vue';

defineOptions({ name: 'SystemBpmRuntimeMyTodoList' });

type TodoActionType = 'approve' | 'complete' | 'reject' | 'return';
type AdvancedActionType =
  | 'addSign'
  | 'delegate'
  | 'recall'
  | 'reduceSign'
  | 'transfer';

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmTaskRecord[]>([]);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailData = ref<BpmTaskDetailRecord>();
const detailLoadErrorMessage = ref('');
const actionDialogVisible = ref(false);
const actionSubmitting = ref(false);
const actionTaskDetail = ref<BpmTaskDetailRecord>();
const taskFormWorkbenchRef = ref<{
  submitPatch: () => Promise<{
    formDataPatchJson?: string;
    formDataVersion?: number;
  }>;
}>();
const employeeLoading = ref(false);
const employeeOptions = ref<EmployeeRecord[]>([]);
const currentActionRow = ref<BpmTaskRecord>();
const advancedDialogVisible = ref(false);
const advancedSubmitting = ref(false);
const currentAdvancedRow = ref<BpmTaskRecord>();

const actionForm = reactive({
  commentText: '',
  copyEmployeeIds: [] as number[],
  type: 'approve' as TodoActionType,
});

const advancedForm = reactive({
  reason: '',
  targetEmployeeId: undefined as number | undefined,
  type: 'transfer' as AdvancedActionType,
});

const searchForm = reactive({
  instanceNo: '',
  instanceTitle: '',
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const columnsFactory = (): ColumnOption<BpmTaskRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'instanceNo', label: '实例编号', minWidth: 150 },
  { prop: 'instanceTitle', label: '流程标题', minWidth: 220 },
  { prop: 'taskName', label: '任务名称', minWidth: 190, useSlot: true },
  { prop: 'assignedAt', label: '到达时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 320,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function getTaskStateLabel(value?: null | number) {
  if (value === 1) {
    return '待处理';
  }
  if (value === 2) {
    return '已完成';
  }
  if (value === 3) {
    return '已取消';
  }
  return '未知';
}

function getTaskStateType(value?: null | number) {
  if (value === 1) {
    return 'warning';
  }
  if (value === 2) {
    return 'success';
  }
  if (value === 3) {
    return 'info';
  }
  return 'info';
}

function getTaskResultLabel(value?: null | number) {
  if (value === 1) {
    return '通过';
  }
  if (value === 2) {
    return '拒绝';
  }
  if (value === 3) {
    return '退回';
  }
  if (value === 4) {
    return '转办';
  }
  return '-';
}

function getTaskResultType(value?: null | number) {
  if (value === 1) {
    return 'success';
  }
  if (value === 2) {
    return 'danger';
  }
  if (value === 3) {
    return 'warning';
  }
  if (value === 4) {
    return 'info';
  }
  return 'info';
}

function getActionLabel(actionType?: null | string) {
  const labelMap: Record<string, string> = {
    ADD_SIGNED: '任务加签',
    APPROVAL_GROUP_ALL_APPROVED: '审批组全员通过',
    APPROVAL_GROUP_CANCELLED: '审批组已关闭',
    APPROVED: '审批通过',
    DELEGATED: '任务委派',
    INSTANCE_CANCELLED: '实例取消',
    PARALLEL_MEMBER_APPROVED: '会签成员通过',
    PARALLEL_MEMBER_REJECTED: '会签成员拒绝，审批组已终止',
    PARALLEL_MEMBER_RETURNED: '会签成员退回发起人，其他待办已取消',
    RECALLED: '发起人撤回',
    REDUCE_SIGNED: '任务减签',
    REJECTED: '审批拒绝',
    RESUBMITTED: '重新提交',
    RETURNED_TO_INITIATOR: '退回发起人',
    TRANSFERRED: '转办',
  };
  return actionType ? (labelMap[actionType] ?? actionType) : '-';
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryMyBpmTodoPage({
      instanceNo: searchForm.instanceNo,
      instanceTitle: searchForm.instanceTitle,
      pageNum: pagination.current,
      pageSize: pagination.size,
    });
    rows.value = result?.list ?? [];
    pagination.total = result?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
  void loadData();
}

function handleReset() {
  Object.assign(searchForm, {
    instanceNo: '',
    instanceTitle: '',
  });
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

async function openDetailDialog(row: BpmTaskRecord) {
  detailVisible.value = true;
  detailLoading.value = true;
  detailData.value = undefined;
  detailLoadErrorMessage.value = '';
  try {
    detailData.value = await getBpmTaskDetail(row.taskId);
  } catch (error: any) {
    detailLoadErrorMessage.value = '流程任务详情加载失败，请稍后重试。';
    ElMessage.error(error?.message || detailLoadErrorMessage.value);
  } finally {
    detailLoading.value = false;
  }
}

function handleCurrentChange(value: number) {
  pagination.current = value;
  void loadData();
}

function handleSizeChange(value: number) {
  pagination.size = value;
  pagination.current = 1;
  void loadData();
}

function getActionDialogTitle() {
  if (actionForm.type === 'approve') {
    return '审批通过';
  }
  if (actionForm.type === 'reject') {
    return '审批拒绝';
  }
  if (actionForm.type === 'complete') {
    return '办理完成';
  }
  return '退回发起人';
}

function getActionPlaceholder() {
  if (actionForm.type === 'approve') {
    return '同意';
  }
  if (actionForm.type === 'reject') {
    return '不同意';
  }
  if (actionForm.type === 'complete') {
    return '办理完成';
  }
  return '请补充材料';
}

async function loadEmployeeOptions(keyword = '') {
  employeeLoading.value = true;
  try {
    const result = await queryEmployeePage({
      disabledFlag: false,
      keyword,
      pageNum: 1,
      pageSize: 20,
    });
    employeeOptions.value = result?.list ?? [];
  } finally {
    employeeLoading.value = false;
  }
}

async function openActionDialog(type: TodoActionType, row: BpmTaskRecord) {
  currentActionRow.value = row;
  Object.assign(actionForm, {
    commentText: '',
    copyEmployeeIds: [],
    type,
  });
  actionTaskDetail.value = undefined;
  if (type === 'approve') {
    try {
      actionTaskDetail.value = await getBpmTaskDetail(row.taskId);
    } catch (error: any) {
      ElMessage.error(error?.message || '审批表单加载失败');
      return;
    }
  }
  actionDialogVisible.value = true;
  await loadEmployeeOptions();
}

function handleApprove(row: BpmTaskRecord) {
  void openActionDialog('approve', row);
}

function handleReject(row: BpmTaskRecord) {
  void openActionDialog('reject', row);
}

function handleReturn(row: BpmTaskRecord) {
  void openActionDialog('return', row);
}

function handleComplete(row: BpmTaskRecord) {
  void openActionDialog('complete', row);
}

function hasTaskAction(row: BpmTaskRecord, action: NonNullable<BpmTaskRecord['availableActions']>[number]) {
  return row.availableActions?.includes(action) === true;
}

function hasAdvancedActions(row: BpmTaskRecord) {
  return ['ADD_SIGN', 'DELEGATE', 'REDUCE_SIGN', 'TRANSFER'].some((action) =>
    row.availableActions?.includes(action as NonNullable<BpmTaskRecord['availableActions']>[number]),
  );
}

async function submitActionDialog() {
  if (!currentActionRow.value) {
    return;
  }
  actionSubmitting.value = true;
  try {
    const payload = {
      commentText: actionForm.commentText,
      copyEmployeeIds: actionForm.copyEmployeeIds,
      taskId: currentActionRow.value.taskId,
    };
    if (actionForm.type === 'approve') {
      const formMutation = await taskFormWorkbenchRef.value?.submitPatch();
      await approveBpmTask({
        ...payload,
        formDataPatchJson: formMutation?.formDataPatchJson,
        formDataVersion: formMutation?.formDataVersion,
      });
      ElMessage.success('审批已通过');
    } else if (actionForm.type === 'complete') {
      await completeBpmTask({
        commentText: actionForm.commentText,
        taskId: currentActionRow.value.taskId,
      });
      ElMessage.success('办理已完成');
    } else if (actionForm.type === 'reject') {
      await rejectBpmTask(payload);
      ElMessage.success('审批已拒绝');
    } else {
      await returnBpmTaskToInitiator(payload);
      ElMessage.success('已退回发起人');
    }
    actionDialogVisible.value = false;
    await loadData();
  } catch (error: any) {
    if (String(error?.message || '').includes('FORM_DATA_VERSION_CONFLICT')) {
      actionTaskDetail.value = await getBpmTaskDetail(
        currentActionRow.value.taskId,
      );
      ElMessage.warning('审批数据已更新，请核对最新内容后重新提交');
      return;
    }
    throw error;
  } finally {
    actionSubmitting.value = false;
  }
}

function getAdvancedActionTitle() {
  const labelMap: Record<AdvancedActionType, string> = {
    addSign: '任务加签',
    delegate: '任务委派',
    recall: '发起人撤回',
    reduceSign: '任务减签',
    transfer: '任务转办',
  };
  return labelMap[advancedForm.type];
}

function advancedActionNeedsEmployee() {
  return ['addSign', 'delegate', 'transfer'].includes(advancedForm.type);
}

async function openAdvancedActionDialog(
  type: AdvancedActionType,
  row: BpmTaskRecord,
) {
  currentAdvancedRow.value = row;
  Object.assign(advancedForm, {
    reason: '',
    targetEmployeeId: undefined,
    type,
  });
  advancedDialogVisible.value = true;
  if (advancedActionNeedsEmployee()) {
    await loadEmployeeOptions();
  }
}

function handleAdvancedCommand(
  command: AdvancedActionType,
  row: BpmTaskRecord,
) {
  void openAdvancedActionDialog(command, row);
}

async function submitAdvancedActionDialog() {
  const row = currentAdvancedRow.value;
  if (!row) {
    return;
  }
  if (advancedActionNeedsEmployee() && !advancedForm.targetEmployeeId) {
    ElMessage.warning('请选择目标员工');
    return;
  }

  advancedSubmitting.value = true;
  try {
    // 五类高级动作复用一个紧凑表单，但请求字段严格按后端契约分流。
    if (advancedForm.type === 'transfer') {
      await transferBpmTask({
        commentText: advancedForm.reason,
        taskId: row.taskId,
        toEmployeeId: advancedForm.targetEmployeeId!,
      });
      ElMessage.success('任务已转办');
    } else if (advancedForm.type === 'delegate') {
      await delegateBpmTask({
        reason: advancedForm.reason,
        targetEmployeeId: advancedForm.targetEmployeeId!,
        taskId: row.taskId,
      });
      ElMessage.success('任务已委派');
    } else if (advancedForm.type === 'addSign') {
      await addSignBpmTask({
        reason: advancedForm.reason,
        targetEmployeeId: advancedForm.targetEmployeeId!,
        taskId: row.taskId,
      });
      ElMessage.success('任务已加签');
    } else if (advancedForm.type === 'reduceSign') {
      await reduceSignBpmTask({
        reason: advancedForm.reason,
        taskId: row.taskId,
      });
      ElMessage.success('任务已减签');
    } else {
      await recallBpmTask({
        reason: advancedForm.reason,
        taskId: row.taskId,
      });
      ElMessage.success('流程已撤回');
    }
    advancedDialogVisible.value = false;
    detailVisible.value = false;
    await loadData();
  } finally {
    advancedSubmitting.value = false;
  }
}

onMounted(() => {
  void loadData().catch((error) => {
    ElMessage.error(error?.message || '我的待办加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="runtime-task-page">
      <ElCard v-show="showSearchBar" class="runtime-task-page__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem label="实例编号">
            <ElInput v-model="searchForm.instanceNo" clearable placeholder="请输入实例编号" />
          </ElFormItem>
          <ElFormItem label="流程标题">
            <ElInput v-model="searchForm.instanceTitle" clearable placeholder="请输入流程标题" />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="runtime-task-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          />
          <ArtTable
            :columns="columns"
            :data="rows"
            :height="tableHeight"
            :loading="loading"
            :pagination="pagination"
            :pagination-options="{
              align: 'center',
              hideOnSinglePage: false,
              layout: 'sizes, prev, pager, next, jumper',
              pageSizes: [10, 20, 30],
              showTotalSummary: true,
              size: 'small',
            }"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #taskName="{ row }">
              <div class="runtime-task-page__task-name">
                <span>{{ row.taskName }}</span>
                <small v-if="row.approvalGroup">
                  {{ row.approvalGroup.approvalGroupName }}，
                  {{ row.approvalGroup.processedMemberCount }}/{{
                    row.approvalGroup.totalMemberCount
                  }}
                  已处理
                </small>
              </div>
            </template>
            <template #actions="{ row }">
              <div class="runtime-task-page__actions">
                <ElButton link size="small" type="primary" @click="openDetailDialog(row)">
                  详情
                </ElButton>
                <ElButton
                  v-if="hasTaskAction(row, 'APPROVE')"
                  link
                  size="small"
                  type="primary"
                  @click="handleApprove(row)"
                >
                  通过
                </ElButton>
                <ElButton
                  v-if="hasTaskAction(row, 'COMPLETE')"
                  link
                  size="small"
                  type="success"
                  @click="handleComplete(row)"
                >
                  完成
                </ElButton>
                <ElButton
                  v-if="hasTaskAction(row, 'REJECT')"
                  link
                  size="small"
                  type="danger"
                  @click="handleReject(row)"
                >
                  拒绝
                </ElButton>
                <ElButton
                  v-if="hasTaskAction(row, 'RETURN')"
                  link
                  size="small"
                  type="warning"
                  @click="handleReturn(row)"
                >
                  退回
                </ElButton>
                <ElDropdown
                  v-if="hasAdvancedActions(row)"
                  trigger="click"
                  @command="(command) => handleAdvancedCommand(command, row)"
                >
                  <ElButton link size="small">高级</ElButton>
                  <template #dropdown>
                    <ElDropdownMenu>
                      <ElDropdownItem v-if="hasTaskAction(row, 'TRANSFER')" command="transfer">转办</ElDropdownItem>
                      <ElDropdownItem v-if="hasTaskAction(row, 'DELEGATE')" command="delegate">委派</ElDropdownItem>
                       <ElDropdownItem
                         v-if="hasTaskAction(row, 'ADD_SIGN')"
                         command="addSign"
                       >
                         加签
                       </ElDropdownItem>
                       <ElDropdownItem
                         v-if="hasTaskAction(row, 'REDUCE_SIGN')"
                         command="reduceSign"
                       >
                         减签
                       </ElDropdownItem>
                    </ElDropdownMenu>
                  </template>
                </ElDropdown>
              </div>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <ElDialog v-model="detailVisible" title="流程任务详情" width="920px">
      <div v-loading="detailLoading" class="runtime-task-page__detail">
        <div v-if="detailLoadErrorMessage" class="runtime-task-page__detail-error">
          <ElEmpty :description="detailLoadErrorMessage" />
        </div>
        <template v-else-if="detailData">
          <ElDescriptions :column="2" border>
            <ElDescriptionsItem label="实例编号">
              {{ detailData.instanceNo }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="流程标题">
              {{ detailData.instanceTitle }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="任务名称">
              {{ detailData.taskName }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="任务标识">
              {{ detailData.taskKey || '-' }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="发起人">
              {{ detailData.startEmployeeNameSnapshot || '-' }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="当前处理人">
              {{ detailData.assigneeNameSnapshot || '-' }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="当前处理部门">
              {{ detailData.assigneeDepartmentNameSnapshot || '-' }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="任务状态">
              <ElTag :type="getTaskStateType(detailData.taskState)" effect="plain" size="small">
                {{ getTaskStateLabel(detailData.taskState) }}
              </ElTag>
            </ElDescriptionsItem>
            <ElDescriptionsItem label="任务结果">
              <ElTag :type="getTaskResultType(detailData.taskResult)" effect="plain" size="small">
                {{ getTaskResultLabel(detailData.taskResult) }}
              </ElTag>
            </ElDescriptionsItem>
            <ElDescriptionsItem label="到达时间">
              {{ detailData.assignedAt || '-' }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="截止时间">
              {{ detailData.dueAt || '-' }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="完成时间">
              {{ detailData.completedAt || '-' }}
            </ElDescriptionsItem>
            <ElDescriptionsItem :span="2" label="运行时分配快照">
              <code>{{ detailData.runtimeAssignmentSnapshotJson || '-' }}</code>
            </ElDescriptionsItem>
          </ElDescriptions>

          <template v-if="detailData.approvalGroup">
            <div class="runtime-task-page__timeline-title">审批组</div>
            <BpmApprovalGroupPanel :group="detailData.approvalGroup" />
          </template>

          <div class="runtime-task-page__timeline-title">动作轨迹</div>
          <ElTimeline v-if="detailData.actionLogs.length > 0" class="runtime-task-page__timeline">
            <ElTimelineItem
              v-for="log in detailData.actionLogs"
              :key="log.actionLogId"
              :timestamp="log.actionAt || ''"
            >
              <div class="runtime-task-page__timeline-row">
                <strong>{{ log.actorNameSnapshot || '-' }}</strong>
                <ElTag effect="plain" size="small">
                  {{ getActionLabel(log.actionType) }}
                </ElTag>
              </div>
              <p v-if="log.commentText" class="runtime-task-page__comment">
                {{ log.commentText }}
              </p>
            </ElTimelineItem>
          </ElTimeline>
          <ElEmpty v-else description="暂无动作轨迹" />
        </template>
      </div>
    </ElDialog>

    <ElDialog
      v-model="actionDialogVisible"
      :title="getActionDialogTitle()"
      :width="actionForm.type === 'approve' ? '860px' : '560px'"
    >
      <div class="runtime-task-page__action-form">
        <BpmTaskFormWorkbench
          v-if="actionForm.type === 'approve'"
          ref="taskFormWorkbenchRef"
          :form-context="actionTaskDetail?.formContext"
        />
        <ElFormItem label="处理意见">
          <ElInput
            v-model="actionForm.commentText"
            :placeholder="getActionPlaceholder()"
            :rows="3"
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="抄送员工">
          <ElSelect
            v-model="actionForm.copyEmployeeIds"
            clearable
            filterable
            :loading="employeeLoading"
            multiple
            placeholder="可选，选择需要知会的员工"
            remote
            :remote-method="loadEmployeeOptions"
            style="width: 100%"
          >
            <ElOption
              v-for="employee in employeeOptions"
              :key="employee.employeeId"
              :label="employee.actualName"
              :value="employee.employeeId"
            />
          </ElSelect>
        </ElFormItem>
      </div>
      <template #footer>
        <ElButton @click="actionDialogVisible = false">取消</ElButton>
        <ElButton :loading="actionSubmitting" type="primary" @click="submitActionDialog">
          确定
        </ElButton>
      </template>
    </ElDialog>

    <ElDialog
      v-model="advancedDialogVisible"
      :title="getAdvancedActionTitle()"
      width="520px"
    >
      <div class="runtime-task-page__action-form">
        <ElFormItem v-if="advancedActionNeedsEmployee()" label="目标员工">
          <ElSelect
            v-model="advancedForm.targetEmployeeId"
            clearable
            filterable
            :loading="employeeLoading"
            placeholder="请选择员工"
            remote
            :remote-method="loadEmployeeOptions"
            style="width: 100%"
          >
            <ElOption
              v-for="employee in employeeOptions"
              :key="employee.employeeId"
              :label="`${employee.actualName}（${employee.departmentName || '未分配部门'}）`"
              :value="employee.employeeId"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="原因">
          <ElInput
            v-model="advancedForm.reason"
            maxlength="500"
            placeholder="可选，请填写操作原因"
            :rows="3"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
      </div>
      <template #footer>
        <ElButton @click="advancedDialogVisible = false">取消</ElButton>
        <ElButton
          :loading="advancedSubmitting"
          type="primary"
          @click="submitAdvancedActionDialog"
        >
          确定
        </ElButton>
      </template>
    </ElDialog>
  </Page>
</template>

<style scoped>
.runtime-task-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.runtime-task-page__search-card,
.runtime-task-page__table-card {
  border-radius: 8px;
}

.runtime-task-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.runtime-task-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.runtime-task-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.runtime-task-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.runtime-task-page :deep(.art-table-panel),
.runtime-task-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.runtime-task-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.runtime-task-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.runtime-task-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.runtime-task-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.runtime-task-page__task-name {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.runtime-task-page__task-name small {
  color: var(--el-text-color-secondary);
  line-height: 18px;
}

.runtime-task-page__detail {
  min-height: 240px;
}

.runtime-task-page__action-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.runtime-task-page__action-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.runtime-task-page__detail-error {
  align-items: center;
  display: flex;
  justify-content: center;
  min-height: 320px;
}

.runtime-task-page__detail code {
  white-space: pre-wrap;
  word-break: break-all;
}

.runtime-task-page__timeline-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  margin-top: 16px;
}

.runtime-task-page__timeline {
  padding-top: 4px;
}

.runtime-task-page__timeline-row {
  align-items: center;
  display: inline-flex;
  gap: 8px;
}

.runtime-task-page__comment {
  color: var(--el-text-color-regular);
  line-height: 22px;
  margin: 6px 0 0;
}
</style>
