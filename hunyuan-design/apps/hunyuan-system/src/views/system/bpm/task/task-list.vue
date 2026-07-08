<script setup lang="ts">
import type { BpmTaskDetailRecord, BpmTaskRecord } from '#/api/system/bpm';
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
  ElOption,
  ElSelect,
  ElTag,
  ElTimeline,
  ElTimelineItem,
} from 'element-plus';

import { getBpmTaskDetail, queryBpmTaskPage } from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmTaskList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmTaskRecord[]>([]);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailData = ref<BpmTaskDetailRecord>();
const detailLoadErrorMessage = ref('');

const searchForm = reactive({
  instanceNo: '',
  instanceTitle: '',
  taskState: undefined as number | undefined,
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
  {
    prop: 'assigneeNameSnapshot',
    label: '当前处理人',
    width: 120,
    align: 'center',
    formatter: (row) => row.assigneeNameSnapshot || '-',
  },
  { prop: 'taskState', label: '任务状态', width: 100, align: 'center', useSlot: true },
  { prop: 'taskResult', label: '任务结果', width: 100, align: 'center', useSlot: true },
  { prop: 'assignedAt', label: '到达时间', minWidth: 180 },
  { prop: 'completedAt', label: '完成时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 120,
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
    const result = await queryBpmTaskPage({
      instanceNo: searchForm.instanceNo,
      instanceTitle: searchForm.instanceTitle,
      pageNum: pagination.current,
      pageSize: pagination.size,
      taskState: searchForm.taskState,
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
    taskState: undefined,
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

onMounted(() => {
  void loadData().catch((error) => {
    ElMessage.error(error?.message || '流程任务数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="task-page">
      <ElCard v-show="showSearchBar" class="task-page__search-card" shadow="never">
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
            <ElInput
              v-model="searchForm.instanceNo"
              clearable
              placeholder="请输入实例编号"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="流程标题">
            <ElInput
              v-model="searchForm.instanceTitle"
              clearable
              placeholder="请输入流程标题"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="任务状态">
            <ElSelect v-model="searchForm.taskState" clearable placeholder="请选择任务状态">
              <ElOption label="待处理" :value="1" />
              <ElOption label="已完成" :value="2" />
              <ElOption label="已取消" :value="3" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="task-page__table-card" shadow="never">
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
            <template #taskState="{ row }">
              <ElTag :type="getTaskStateType(row.taskState)" effect="plain" size="small">
                {{ getTaskStateLabel(row.taskState) }}
              </ElTag>
            </template>

            <template #taskResult="{ row }">
              <ElTag :type="getTaskResultType(row.taskResult)" effect="plain" size="small">
                {{ getTaskResultLabel(row.taskResult) }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <div class="task-page__actions">
                <ElButton link size="small" type="primary" @click="openDetailDialog(row)">
                  详情
                </ElButton>
              </div>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <ElDialog v-model="detailVisible" title="流程任务详情" width="920px">
      <div v-loading="detailLoading" class="task-page__detail">
        <div v-if="detailLoadErrorMessage" class="task-page__detail-error">
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

          <div class="task-page__timeline-title">动作轨迹</div>
          <ElTimeline v-if="detailData.actionLogs.length > 0" class="task-page__timeline">
            <ElTimelineItem
              v-for="log in detailData.actionLogs"
              :key="log.actionLogId"
              :timestamp="log.actionAt || ''"
            >
              <div class="task-page__timeline-row">
                <strong>{{ log.actorNameSnapshot || '-' }}</strong>
                <ElTag effect="plain" size="small">
                  {{ getActionLabel(log.actionType) }}
                </ElTag>
              </div>
              <p v-if="log.commentText" class="task-page__comment">
                {{ log.commentText }}
              </p>
            </ElTimelineItem>
          </ElTimeline>
          <ElEmpty v-else description="暂无动作轨迹" />
        </template>
      </div>
    </ElDialog>
  </Page>
</template>

<style scoped>
.task-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.task-page__search-card,
.task-page__table-card {
  border-radius: 8px;
}

.task-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.task-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.task-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.task-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.task-page :deep(.art-table-panel),
.task-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.task-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.task-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.task-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.task-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.task-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.task-page__detail {
  min-height: 240px;
}

.task-page__detail-error {
  align-items: center;
  display: flex;
  justify-content: center;
  min-height: 320px;
}

.task-page__detail code {
  white-space: pre-wrap;
  word-break: break-all;
}

.task-page__timeline-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  margin-top: 16px;
}

.task-page__timeline {
  padding-top: 4px;
}

.task-page__timeline-row {
  align-items: center;
  display: inline-flex;
  gap: 8px;
}

.task-page__comment {
  color: var(--el-text-color-regular);
  line-height: 22px;
  margin: 6px 0 0;
}
</style>
