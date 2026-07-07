<script setup lang="ts">
import type { BpmTaskRecord } from '#/api/system/bpm';
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

import { ElCard, ElFormItem, ElInput, ElMessage, ElOption, ElSelect, ElTag } from 'element-plus';

import { queryBpmTaskPage } from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmTaskList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmTaskRecord[]>([]);

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
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
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
</style>
