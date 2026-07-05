<script setup lang="ts">
import type {
  OperateLogPageQueryParams,
  OperateLogRecord,
} from '#/api/system/operate-log';
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, onMounted, reactive, ref } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  useTableColumns,
} from '@vben/art-hooks/table';
import { Page, useVbenDrawer } from '@vben/common-ui';

import {
  ElButton,
  ElCard,
  ElDatePicker,
  ElFormItem,
  ElInput,
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

import { queryOperateLogPage } from '#/api/system/operate-log';

import OperateLogDetailDrawerPanel from './components/operate-log-detail-drawer.vue';

defineOptions({ name: 'SystemNetworkSecurityOperateLogList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<OperateLogRecord[]>([]);
const currentDetailRow = ref<OperateLogRecord>();

const [OperateLogDetailDrawer, operateLogDetailDrawerApi] = useVbenDrawer({
  connectedComponent: OperateLogDetailDrawerPanel,
  destroyOnClose: false,
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchForm = reactive({
  dateRange: [] as string[],
  keywords: '',
  requestKeywords: '',
  successFlag: undefined as boolean | undefined,
  userName: '',
});

const columnsFactory = (): ColumnOption<OperateLogRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  {
    prop: 'operateUserName',
    label: '操作人',
    minWidth: 140,
    formatter: (row) => row.operateUserName || '-',
  },
  {
    prop: 'module',
    label: '模块',
    minWidth: 160,
    formatter: (row) => row.module || '-',
  },
  {
    prop: 'content',
    label: '操作内容',
    minWidth: 220,
    formatter: (row) => row.content || '-',
  },
  {
    prop: 'method',
    label: '请求方法',
    width: 110,
    align: 'center',
    formatter: (row) => row.method || '-',
  },
  {
    prop: 'url',
    label: '请求地址',
    minWidth: 220,
    formatter: (row) => row.url || '-',
  },
  {
    prop: 'successFlag',
    label: '结果',
    width: 100,
    align: 'center',
    useSlot: true,
  },
  {
    prop: 'ip',
    label: '客户端 IP',
    minWidth: 140,
    formatter: (row) => row.ip || '-',
  },
  {
    prop: 'createTime',
    label: '记录时间',
    minWidth: 180,
    formatter: (row) => row.createTime || '-',
  },
  {
    prop: 'actions',
    label: '操作',
    width: 96,
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

function buildQueryParams(): OperateLogPageQueryParams {
  return {
    endDate: searchForm.dateRange[1] || '',
    keywords: searchForm.keywords,
    pageNum: pagination.current,
    pageSize: pagination.size,
    requestKeywords: searchForm.requestKeywords,
    startDate: searchForm.dateRange[0] || '',
    successFlag: searchForm.successFlag,
    userName: searchForm.userName,
  };
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryOperateLogPage(buildQueryParams());
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
    dateRange: [],
    keywords: '',
    requestKeywords: '',
    successFlag: undefined,
    userName: '',
  });
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function openOperateLogDetail(row: OperateLogRecord) {
  currentDetailRow.value = row;
  operateLogDetailDrawerApi.open();
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
  void loadData();
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="operate-log-page">
      <ElCard
        v-show="showSearchBar"
        class="operate-log-page__search-card"
        shadow="never"
      >
        <ArtSearchPanel
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem class="operate-log-page__name-item" label="操作人">
            <ElInput
              v-model="searchForm.userName"
              clearable
              placeholder="请输入操作人"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="operate-log-page__keyword-item" label="业务关键字">
            <ElInput
              v-model="searchForm.keywords"
              clearable
              placeholder="请输入模块或操作内容"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="operate-log-page__keyword-item" label="请求关键字">
            <ElInput
              v-model="searchForm.requestKeywords"
              clearable
              placeholder="请输入请求地址、方法或参数"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="operate-log-page__status-item" label="执行结果">
            <ElSelect
              v-model="searchForm.successFlag"
              clearable
              placeholder="请选择执行结果"
            >
              <ElOption :value="true" label="成功" />
              <ElOption :value="false" label="失败" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem class="operate-log-page__date-item" label="记录日期">
            <ElDatePicker
              v-model="searchForm.dateRange"
              end-placeholder="结束日期"
              range-separator="至"
              start-placeholder="开始日期"
              type="daterange"
              value-format="YYYY-MM-DD"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="operate-log-page__table-card" shadow="never">
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
            row-key="operateLogId"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #successFlag="{ row }">
              <ElTag
                :type="row.successFlag ? 'success' : 'danger'"
                effect="plain"
                size="small"
              >
                {{ row.successFlag ? '成功' : '失败' }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <ElSpace class="operate-log-page__actions">
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openOperateLogDetail(row)"
                >
                  详情
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>

      <OperateLogDetailDrawer :operate-log="currentDetailRow" />
    </div>
  </Page>
</template>

<style scoped>
.operate-log-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.operate-log-page__search-card,
.operate-log-page__table-card {
  border-radius: 8px;
}

.operate-log-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.operate-log-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.operate-log-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.operate-log-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.operate-log-page :deep(.art-table-panel),
.operate-log-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.operate-log-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.operate-log-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.operate-log-page__name-item :deep(.el-form-item__content),
.operate-log-page__keyword-item :deep(.el-form-item__content) {
  width: 220px;
}

.operate-log-page__status-item :deep(.el-form-item__content) {
  width: 168px;
}

.operate-log-page__date-item :deep(.el-form-item__content) {
  width: 320px;
}

.operate-log-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.operate-log-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.operate-log-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .operate-log-page__name-item :deep(.el-form-item__content),
  .operate-log-page__keyword-item :deep(.el-form-item__content),
  .operate-log-page__status-item :deep(.el-form-item__content),
  .operate-log-page__date-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
