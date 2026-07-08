<script setup lang="ts">
import type { BpmCommandRecordVO } from '#/api/system/bpm';
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
  ElCard,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElSelect,
  ElTag,
} from 'element-plus';

import { queryBpmCommandRecordPage } from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmIntegrationCommandRecordList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmCommandRecordVO[]>([]);

const searchForm = reactive({
  businessType: '',
  commandKey: '',
  commandStatus: undefined as number | undefined,
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const columnsFactory = (): ColumnOption<BpmCommandRecordVO>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'commandKey', label: '命令键', minWidth: 220 },
  { prop: 'commandType', label: '命令类型', width: 110, align: 'center' },
  { prop: 'businessType', label: '业务类型', width: 120, align: 'center' },
  { prop: 'businessId', label: '业务ID', width: 120, align: 'center' },
  { prop: 'instanceId', label: '实例ID', width: 110, align: 'center' },
  { prop: 'commandStatus', label: '命令状态', width: 110, align: 'center', useSlot: true },
  { prop: 'failureReason', label: '失败原因', minWidth: 220 },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function getCommandStatusLabel(value?: null | number) {
  if (value === 0) {
    return '处理中';
  }
  if (value === 1) {
    return '成功';
  }
  if (value === 2) {
    return '失败';
  }
  return '未知';
}

function getCommandStatusType(value?: null | number) {
  if (value === 1) {
    return 'success';
  }
  if (value === 2) {
    return 'danger';
  }
  return 'warning';
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryBpmCommandRecordPage({
      businessType: searchForm.businessType,
      commandKey: searchForm.commandKey,
      commandStatus: searchForm.commandStatus,
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
    businessType: '',
    commandKey: '',
    commandStatus: undefined,
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
    ElMessage.error(error?.message || '命令记录加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="command-record-page">
      <ElCard v-show="showSearchBar" class="command-record-page__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem label="命令键">
            <ElInput
              v-model="searchForm.commandKey"
              clearable
              placeholder="请输入命令键"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="业务类型">
            <ElInput
              v-model="searchForm.businessType"
              clearable
              placeholder="请输入业务类型"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="命令状态">
            <ElSelect v-model="searchForm.commandStatus" clearable placeholder="请选择状态">
              <ElOption label="处理中" :value="0" />
              <ElOption label="成功" :value="1" />
              <ElOption label="失败" :value="2" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="command-record-page__table-card" shadow="never">
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
            <template #commandStatus="{ row }">
              <ElTag :type="getCommandStatusType(row.commandStatus)" effect="plain" size="small">
                {{ getCommandStatusLabel(row.commandStatus) }}
              </ElTag>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.command-record-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.command-record-page__search-card,
.command-record-page__table-card {
  border-radius: 8px;
}

.command-record-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.command-record-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.command-record-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.command-record-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.command-record-page :deep(.art-table-panel),
.command-record-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.command-record-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.command-record-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}
</style>
