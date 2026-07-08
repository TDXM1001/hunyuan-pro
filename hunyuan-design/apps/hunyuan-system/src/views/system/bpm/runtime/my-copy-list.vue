<script setup lang="ts">
import type { BpmInstanceCopyRecord } from '#/api/system/bpm';
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
  ElTag,
} from 'element-plus';

import { markBpmCopyRead, queryMyBpmCopyPage } from '#/api/system/bpm';

import BpmInstanceDetailDrawer from './components/bpm-instance-detail-drawer.vue';

defineOptions({ name: 'SystemBpmRuntimeMyCopyList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmInstanceCopyRecord[]>([]);
const detailDrawerRef = ref<InstanceType<typeof BpmInstanceDetailDrawer>>();

const searchForm = reactive({
  instanceNo: '',
  readState: undefined as number | undefined,
  title: '',
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const columnsFactory = (): ColumnOption<BpmInstanceCopyRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'instanceNo', label: '实例编号', minWidth: 150 },
  { prop: 'title', label: '流程标题', minWidth: 220 },
  { prop: 'copyType', label: '抄送类型', minWidth: 150, useSlot: true },
  { prop: 'sourceNodeName', label: '来源节点', minWidth: 150 },
  {
    prop: 'reasonSnapshot',
    label: '抄送原因',
    minWidth: 220,
    showOverflowTooltip: true,
  },
  {
    prop: 'readState',
    label: '已读状态',
    width: 110,
    align: 'center',
    useSlot: true,
  },
  { prop: 'sentAt', label: '发送时间', minWidth: 180 },
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

function getCopyTypeLabel(value?: null | string) {
  const labelMap: Record<string, string> = {
    MANUAL_APPROVE_COPY: '审批通过抄送',
    MANUAL_REJECT_COPY: '审批拒绝抄送',
    MANUAL_RETURN_COPY: '退回发起人抄送',
  };
  return value ? (labelMap[value] ?? value) : '-';
}

function getReadStateLabel(value?: null | number) {
  return value === 1 ? '已读' : '未读';
}

function getReadStateType(value?: null | number) {
  return value === 1 ? 'info' : 'warning';
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryMyBpmCopyPage({
      instanceNo: searchForm.instanceNo,
      pageNum: pagination.current,
      pageSize: pagination.size,
      readState: searchForm.readState,
      title: searchForm.title,
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
    readState: undefined,
    title: '',
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

async function openDetail(row: BpmInstanceCopyRecord) {
  await markBpmCopyRead(row.copyId);
  await loadData();
  detailDrawerRef.value?.open(row.instanceId);
}

onMounted(() => {
  void loadData().catch((error) => {
    ElMessage.error(error?.message || '我的抄送加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="runtime-copy-page">
      <ElCard v-show="showSearchBar" class="runtime-copy-page__search-card" shadow="never">
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
            <ElInput v-model="searchForm.title" clearable placeholder="请输入流程标题" />
          </ElFormItem>
          <ElFormItem label="已读状态">
            <ElSelect v-model="searchForm.readState" clearable placeholder="请选择已读状态">
              <ElOption label="未读" :value="0" />
              <ElOption label="已读" :value="1" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="runtime-copy-page__table-card" shadow="never">
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
            <template #copyType="{ row }">
              <ElTag effect="plain" size="small">{{ getCopyTypeLabel(row.copyType) }}</ElTag>
            </template>
            <template #readState="{ row }">
              <ElTag :type="getReadStateType(row.readState)" effect="plain" size="small">
                {{ getReadStateLabel(row.readState) }}
              </ElTag>
            </template>
            <template #actions="{ row }">
              <div class="runtime-copy-page__actions">
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
.runtime-copy-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.runtime-copy-page__search-card,
.runtime-copy-page__table-card {
  border-radius: 8px;
}

.runtime-copy-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.runtime-copy-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.runtime-copy-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.runtime-copy-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.runtime-copy-page :deep(.art-table-panel),
.runtime-copy-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.runtime-copy-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.runtime-copy-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.runtime-copy-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.runtime-copy-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
