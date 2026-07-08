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
  ElEmpty,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTag,
  ElTimeline,
  ElTimelineItem,
} from 'element-plus';

import {
  approveBpmTask,
  getBpmTaskDetail,
  queryMyBpmTodoPage,
  rejectBpmTask,
  returnBpmTaskToInitiator,
  transferBpmTask,
} from '#/api/system/bpm';
import { queryEmployeePage } from '#/api/system/organization';

defineOptions({ name: 'SystemBpmRuntimeMyTodoList' });

type TodoActionType = 'approve' | 'reject' | 'return';

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmTaskRecord[]>([]);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailData = ref<BpmTaskDetailRecord>();
const detailLoadErrorMessage = ref('');
const actionDialogVisible = ref(false);
const actionSubmitting = ref(false);
const employeeLoading = ref(false);
const employeeOptions = ref<EmployeeRecord[]>([]);
const currentActionRow = ref<BpmTaskRecord>();

const actionForm = reactive({
  commentText: '',
  copyEmployeeIds: [] as number[],
  type: 'approve' as TodoActionType,
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
  { prop: 'taskName', label: '任务名称', minWidth: 160 },
  { prop: 'assignedAt', label: '到达时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 280,
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
    APPROVED: '审批通过',
    INSTANCE_CANCELLED: '实例取消',
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
  return '退回发起人';
}

function getActionPlaceholder() {
  if (actionForm.type === 'approve') {
    return '同意';
  }
  if (actionForm.type === 'reject') {
    return '不同意';
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
      await approveBpmTask(payload);
      ElMessage.success('审批已通过');
    } else if (actionForm.type === 'reject') {
      await rejectBpmTask(payload);
      ElMessage.success('审批已拒绝');
    } else {
      await returnBpmTaskToInitiator(payload);
      ElMessage.success('已退回发起人');
    }
    actionDialogVisible.value = false;
    await loadData();
  } finally {
    actionSubmitting.value = false;
  }
}

async function handleTransfer(row: BpmTaskRecord) {
  const { value } = await ElMessageBox.prompt('请输入接收人员工ID', '转办任务', {
    inputPattern: /^\d+$/,
    inputPlaceholder: '员工ID',
    inputErrorMessage: '请输入数字员工ID',
  });
  await transferBpmTask({
    commentText: '转办',
    taskId: row.taskId,
    toEmployeeId: Number(value),
  });
  ElMessage.success('任务已转办');
  await loadData();
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
            <template #actions="{ row }">
              <div class="runtime-task-page__actions">
                <ElButton link size="small" type="primary" @click="openDetailDialog(row)">
                  详情
                </ElButton>
                <ElButton link size="small" type="primary" @click="handleApprove(row)">
                  通过
                </ElButton>
                <ElButton link size="small" type="danger" @click="handleReject(row)">
                  拒绝
                </ElButton>
                <ElButton link size="small" type="warning" @click="handleReturn(row)">
                  退回
                </ElButton>
                <ElButton link size="small" @click="handleTransfer(row)">转办</ElButton>
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

    <ElDialog v-model="actionDialogVisible" :title="getActionDialogTitle()" width="560px">
      <div class="runtime-task-page__action-form">
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
