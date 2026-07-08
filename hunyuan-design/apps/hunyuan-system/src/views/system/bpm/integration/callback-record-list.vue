<script setup lang="ts">
import type { BpmCallbackRecordVO } from '#/api/system/bpm';
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
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

import {
  queryBpmCallbackRecordPage,
  retryBpmCallbackRecord,
} from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmIntegrationCallbackRecordList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmCallbackRecordVO[]>([]);

const searchForm = reactive({
  businessType: '',
  callbackStatus: undefined as number | undefined,
  eventId: '',
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const columnsFactory = (): ColumnOption<BpmCallbackRecordVO>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'eventId', label: '事件ID', minWidth: 180 },
  { prop: 'businessType', label: '业务类型', width: 120, align: 'center' },
  { prop: 'businessId', label: '业务ID', width: 120, align: 'center' },
  { prop: 'instanceId', label: '实例ID', width: 110, align: 'center' },
  { prop: 'callbackStatus', label: '回调状态', width: 110, align: 'center', useSlot: true },
  { prop: 'retryCount', label: '重试次数', width: 100, align: 'center' },
  { prop: 'failureReason', label: '失败原因', minWidth: 220 },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 100,
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

function getCallbackStatusLabel(value?: null | number) {
  if (value === 0) {
    return '待回调';
  }
  if (value === 1) {
    return '成功';
  }
  if (value === 2) {
    return '失败';
  }
  return '未知';
}

function getCallbackStatusType(value?: null | number) {
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
    const result = await queryBpmCallbackRecordPage({
      businessType: searchForm.businessType,
      callbackStatus: searchForm.callbackStatus,
      eventId: searchForm.eventId,
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
    callbackStatus: undefined,
    eventId: '',
  });
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

async function handleRetry(row: BpmCallbackRecordVO) {
  await retryBpmCallbackRecord(row.callbackRecordId);
  ElMessage.success('重试请求已提交');
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

onMounted(() => {
  void loadData().catch((error) => {
    ElMessage.error(error?.message || '回调记录加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="callback-record-page">
      <ElCard v-show="showSearchBar" class="callback-record-page__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem label="事件ID">
            <ElInput
              v-model="searchForm.eventId"
              clearable
              placeholder="请输入事件ID"
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
          <ElFormItem label="回调状态">
            <ElSelect v-model="searchForm.callbackStatus" clearable placeholder="请选择状态">
              <ElOption label="待回调" :value="0" />
              <ElOption label="成功" :value="1" />
              <ElOption label="失败" :value="2" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="callback-record-page__table-card" shadow="never">
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
            <template #callbackStatus="{ row }">
              <ElTag :type="getCallbackStatusType(row.callbackStatus)" effect="plain" size="small">
                {{ getCallbackStatusLabel(row.callbackStatus) }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <ElSpace class="callback-record-page__actions">
                <ElButton
                  v-if="row.callbackStatus === 2"
                  link
                  size="small"
                  type="primary"
                  @click="handleRetry(row)"
                >
                  重试
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.callback-record-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.callback-record-page__search-card,
.callback-record-page__table-card {
  border-radius: 8px;
}

.callback-record-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.callback-record-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.callback-record-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.callback-record-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.callback-record-page :deep(.art-table-panel),
.callback-record-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.callback-record-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.callback-record-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.callback-record-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.callback-record-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.callback-record-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
