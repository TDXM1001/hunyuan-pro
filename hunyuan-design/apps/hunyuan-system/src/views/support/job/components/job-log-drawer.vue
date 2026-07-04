<script setup lang="ts">
import type {
  JobLogQueryParams,
  JobLogRecord,
  JobRecord,
} from '#/api/system/job';
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, reactive, ref, watch } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  useTableColumns,
} from '@vben/art-hooks/table';
import { useVbenDrawer } from '@vben/common-ui';

import {
  ElCard,
  ElDatePicker,
  ElFormItem,
  ElInput,
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

import { queryJobLogs } from '#/api/system/job';

defineOptions({ name: 'SystemSupportJobLogDrawer' });

const props = defineProps<{
  job?: JobRecord;
}>();

const [Drawer, drawerApi] = useVbenDrawer();
const drawerOpen = drawerApi.useStore((state) => Boolean(state.isOpen));

const loading = ref(false);
const rows = ref<JobLogRecord[]>([]);
const lastLoadedJobId = ref<null | number>(null);
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchForm = reactive<JobLogQueryParams>({
  endTime: '',
  jobId: undefined,
  pageNum: 1,
  pageSize: 10,
  searchWord: '',
  startTime: '',
  successFlag: undefined,
});

const columnsFactory = (): ColumnOption<JobLogRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  {
    prop: 'successFlag',
    label: '执行结果',
    width: 100,
    align: 'center',
    useSlot: true,
  },
  {
    prop: 'executeResult',
    label: '结果描述',
    minWidth: 260,
    formatter: (row) => row.executeResult || '-',
  },
  { prop: 'executeStartTime', label: '开始时间', minWidth: 180 },
  { prop: 'executeEndTime', label: '结束时间', minWidth: 180 },
  {
    prop: 'executeTimeMillis',
    label: '耗时(ms)',
    width: 110,
    align: 'center',
    formatter: (row) =>
      row.executeTimeMillis == null ? '-' : `${row.executeTimeMillis}`,
  },
  {
    prop: 'param',
    label: '执行参数',
    minWidth: 220,
    formatter: (row) => row.param || '-',
  },
  {
    prop: 'ip',
    label: '执行节点',
    minWidth: 150,
    formatter: (row) => row.ip || '-',
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const drawerTitle = computed(() =>
  props.job?.jobName ? `${props.job.jobName} - 执行日志` : '执行日志',
);
const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function resetFilters() {
  Object.assign(searchForm, {
    endTime: '',
    searchWord: '',
    startTime: '',
    successFlag: undefined,
  });
}

async function loadData() {
  if (!props.job?.jobId) {
    rows.value = [];
    pagination.total = 0;
    return;
  }

  loading.value = true;
  try {
    const result = await queryJobLogs({
      ...searchForm,
      jobId: props.job.jobId,
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
  resetFilters();
  pagination.current = 1;
  void loadData();
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

watch(
  () => [props.job?.jobId, drawerOpen.value] as const,
  ([jobId, isOpen]) => {
    if (!isOpen) {
      return;
    }

    if (!jobId) {
      rows.value = [];
      pagination.total = 0;
      return;
    }

    if (lastLoadedJobId.value !== jobId) {
      lastLoadedJobId.value = jobId;
      resetFilters();
      pagination.current = 1;
    }

    void loadData();
  },
  { immediate: true },
);
</script>

<template>
  <Drawer
    class="w-[1280px] max-w-[calc(100vw-24px)]"
    close-icon-placement="left"
    content-class="!p-0"
    :footer="false"
    :title="drawerTitle"
  >
    <div class="job-log-drawer">
      <ElCard class="job-log-drawer__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem class="job-log-drawer__keyword-item" label="关键字">
            <ElInput
              v-model="searchForm.searchWord"
              clearable
              placeholder="请输入结果或参数"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="job-log-drawer__status-item" label="执行结果">
            <ElSelect
              v-model="searchForm.successFlag"
              clearable
              placeholder="请选择执行结果"
            >
              <ElOption :value="true" label="成功" />
              <ElOption :value="false" label="失败" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem class="job-log-drawer__date-item" label="开始日期">
            <ElDatePicker
              v-model="searchForm.startTime"
              placeholder="请选择开始日期"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </ElFormItem>
          <ElFormItem class="job-log-drawer__date-item" label="结束日期">
            <ElDatePicker
              v-model="searchForm.endTime"
              placeholder="请选择结束日期"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="job-log-drawer__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="size,fullscreen,columns,settings"
          >
            <template #left>
              <ElSpace class="job-log-drawer__meta">
                <span>任务 ID：{{ props.job?.jobId || '-' }}</span>
                <span>任务名称：{{ props.job?.jobName || '-' }}</span>
              </ElSpace>
            </template>
          </ArtTableHeader>

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
            row-key="logId"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #successFlag="{ row }">
              <ElTag
                effect="plain"
                size="small"
                :type="row.successFlag ? 'success' : 'danger'"
              >
                {{ row.successFlag ? '成功' : '失败' }}
              </ElTag>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Drawer>
</template>

<style scoped>
.job-log-drawer {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.job-log-drawer__search-card,
.job-log-drawer__table-card {
  border-radius: 8px;
}

.job-log-drawer__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.job-log-drawer__search-card :deep(.el-card__body) {
  padding: 16px;
}

.job-log-drawer__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.job-log-drawer__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.job-log-drawer :deep(.art-table-panel),
.job-log-drawer :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.job-log-drawer :deep(.art-table-header) {
  margin-bottom: 18px;
}

.job-log-drawer :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.job-log-drawer__meta {
  color: var(--el-text-color-regular);
  flex-wrap: wrap;
  font-size: 14px;
  line-height: 22px;
}

.job-log-drawer__keyword-item :deep(.el-form-item__content) {
  width: 320px;
}

.job-log-drawer__status-item :deep(.el-form-item__content),
.job-log-drawer__date-item :deep(.el-form-item__content) {
  width: 180px;
}

@media (width <= 768px) {
  .job-log-drawer__keyword-item :deep(.el-form-item__content),
  .job-log-drawer__status-item :deep(.el-form-item__content),
  .job-log-drawer__date-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
