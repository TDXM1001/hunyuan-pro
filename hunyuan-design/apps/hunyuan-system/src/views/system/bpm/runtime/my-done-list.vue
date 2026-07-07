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

import {
  ElButton,
  ElCard,
  ElFormItem,
  ElInput,
  ElMessage,
  ElTag,
} from 'element-plus';

import { queryMyBpmDonePage } from '#/api/system/bpm';

import BpmInstanceDetailDrawer from './components/bpm-instance-detail-drawer.vue';

defineOptions({ name: 'SystemBpmRuntimeMyDoneList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmTaskRecord[]>([]);
const detailDrawerRef = ref<InstanceType<typeof BpmInstanceDetailDrawer>>();

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

function getTaskResultLabel(value?: null | number) {
  if (value === 1) return '通过';
  if (value === 2) return '拒绝';
  if (value === 3) return '退回';
  if (value === 4) return '取消';
  return '-';
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryMyBpmDonePage({
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

function handleCurrentChange(value: number) {
  pagination.current = value;
  void loadData();
}

function handleSizeChange(value: number) {
  pagination.size = value;
  pagination.current = 1;
  void loadData();
}

function openDetail(row: BpmTaskRecord) {
  void detailDrawerRef.value?.open(row.instanceId);
}

onMounted(() => {
  void loadData().catch((error) => {
    ElMessage.error(error?.message || '我的已办加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="runtime-done-page">
      <ElCard v-show="showSearchBar" class="runtime-done-page__search-card" shadow="never">
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

      <ElCard class="runtime-done-page__table-card" shadow="never">
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
            <template #taskResult="{ row }">
              <ElTag effect="plain" size="small">
                {{ getTaskResultLabel(row.taskResult) }}
              </ElTag>
            </template>
            <template #actions="{ row }">
              <div class="runtime-done-page__actions">
                <ElButton link size="small" type="primary" @click="openDetail(row)">
                  详情
                </ElButton>
              </div>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
    <BpmInstanceDetailDrawer ref="detailDrawerRef" />
  </Page>
</template>

<style scoped>
.runtime-done-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.runtime-done-page__search-card,
.runtime-done-page__table-card {
  border-radius: 8px;
}

.runtime-done-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.runtime-done-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.runtime-done-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.runtime-done-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.runtime-done-page :deep(.art-table-panel),
.runtime-done-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.runtime-done-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.runtime-done-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.runtime-done-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}
</style>
