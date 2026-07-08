<script setup lang="ts">
import type { BpmInstanceRecord } from '#/api/system/bpm';
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

import { queryBpmInstancePage } from '#/api/system/bpm';

import BpmInstanceDetailDrawer from '../runtime/components/bpm-instance-detail-drawer.vue';

defineOptions({ name: 'SystemBpmInstanceList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmInstanceRecord[]>([]);
const detailDrawerRef = ref<InstanceType<typeof BpmInstanceDetailDrawer>>();

const searchForm = reactive({
  instanceNo: '',
  runState: undefined as number | undefined,
  title: '',
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const columnsFactory = (): ColumnOption<BpmInstanceRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'instanceNo', label: '实例编号', minWidth: 160 },
  { prop: 'title', label: '流程标题', minWidth: 220 },
  { prop: 'startEmployeeNameSnapshot', label: '发起人', width: 120, align: 'center' },
  { prop: 'runState', label: '运行状态', width: 100, align: 'center', useSlot: true },
  { prop: 'resultState', label: '结果状态', width: 100, align: 'center', useSlot: true },
  { prop: 'startedAt', label: '发起时间', minWidth: 180 },
  { prop: 'finishedAt', label: '结束时间', minWidth: 180 },
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

function getRunStateLabel(value?: null | number) {
  if (value === 1) {
    return '运行中';
  }
  if (value === 2) {
    return '待重交';
  }
  if (value === 3) {
    return '已结束';
  }
  if (value === 4) {
    return '已取消';
  }
  return '未知';
}

function getRunStateType(value?: null | number) {
  if (value === 1) {
    return 'warning';
  }
  if (value === 2) {
    return 'warning';
  }
  if (value === 3) {
    return 'success';
  }
  if (value === 4) {
    return 'info';
  }
  return 'info';
}

function getResultStateLabel(value?: null | number) {
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
    return '取消';
  }
  return '-';
}

function getResultStateType(value?: null | number) {
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
    const result = await queryBpmInstancePage({
      instanceNo: searchForm.instanceNo,
      pageNum: pagination.current,
      pageSize: pagination.size,
      runState: searchForm.runState,
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
    runState: undefined,
    title: '',
  });
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function openDetail(row: BpmInstanceRecord) {
  void detailDrawerRef.value?.open(row.instanceId, 'admin');
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
    ElMessage.error(error?.message || '流程实例数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="instance-page">
      <ElCard v-show="showSearchBar" class="instance-page__search-card" shadow="never">
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
              v-model="searchForm.title"
              clearable
              placeholder="请输入流程标题"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="运行状态">
            <ElSelect v-model="searchForm.runState" clearable placeholder="请选择运行状态">
              <ElOption label="运行中" :value="1" />
              <ElOption label="待重交" :value="2" />
              <ElOption label="已结束" :value="3" />
              <ElOption label="已取消" :value="4" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="instance-page__table-card" shadow="never">
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
            <template #runState="{ row }">
              <ElTag :type="getRunStateType(row.runState)" effect="plain" size="small">
                {{ getRunStateLabel(row.runState) }}
              </ElTag>
            </template>

            <template #resultState="{ row }">
              <ElTag :type="getResultStateType(row.resultState)" effect="plain" size="small">
                {{ getResultStateLabel(row.resultState) }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <ElSpace class="instance-page__actions">
                <ElButton link size="small" type="primary" @click="openDetail(row)">
                  详情
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <BpmInstanceDetailDrawer ref="detailDrawerRef" />
  </Page>
</template>

<style scoped>
.instance-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.instance-page__search-card,
.instance-page__table-card {
  border-radius: 8px;
}

.instance-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.instance-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.instance-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.instance-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.instance-page :deep(.art-table-panel),
.instance-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.instance-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.instance-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.instance-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.instance-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.instance-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
