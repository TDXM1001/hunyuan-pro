<script setup lang="ts">
import type {
  LoginLogPageQueryParams,
  LoginLogRecord,
} from '#/api/system/login-log';
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
  ElDatePicker,
  ElFormItem,
  ElInput,
  ElTag,
} from 'element-plus';

import { queryLoginLogPage } from '#/api/system/login-log';

defineOptions({ name: 'SystemNetworkSecurityLoginLogList' });

const resultLabelMap: Record<number, string> = {
  0: '登录成功',
  1: '登录失败',
  2: '退出登录',
};

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<LoginLogRecord[]>([]);

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchForm = reactive({
  dateRange: [] as string[],
  ip: '',
  userName: '',
});

const columnsFactory = (): ColumnOption<LoginLogRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  {
    prop: 'userName',
    label: '用户名称',
    minWidth: 160,
    formatter: (row) => row.userName || '-',
  },
  {
    prop: 'loginIp',
    label: '登录 IP',
    minWidth: 140,
    formatter: (row) => row.loginIp || '-',
  },
  {
    prop: 'loginIpRegion',
    label: 'IP 地区',
    minWidth: 160,
    formatter: (row) => row.loginIpRegion || '-',
  },
  {
    prop: 'loginDevice',
    label: '登录设备',
    minWidth: 120,
    formatter: (row) => row.loginDevice || '-',
  },
  { prop: 'loginResult', label: '结果', width: 100, align: 'center', useSlot: true },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 200,
    formatter: (row) => row.remark || '-',
  },
  {
    prop: 'createTime',
    label: '记录时间',
    minWidth: 180,
    formatter: (row) => row.createTime || '-',
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function buildQueryParams(): LoginLogPageQueryParams {
  return {
    endDate: searchForm.dateRange[1] || '',
    ip: searchForm.ip,
    pageNum: pagination.current,
    pageSize: pagination.size,
    startDate: searchForm.dateRange[0] || '',
    userName: searchForm.userName,
  };
}

function resolveResultLabel(value?: null | number) {
  if (value === undefined || value === null) {
    return '-';
  }
  return resultLabelMap[value] || `未知结果(${value})`;
}

function resolveResultType(value?: null | number) {
  if (value === 0) {
    return 'success';
  }
  if (value === 1) {
    return 'danger';
  }
  return 'info';
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryLoginLogPage(buildQueryParams());
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
    ip: '',
    userName: '',
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
  void loadData();
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="login-log-page">
      <ElCard
        v-show="showSearchBar"
        class="login-log-page__search-card"
        shadow="never"
      >
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem class="login-log-page__name-item" label="用户名称">
            <ElInput
              v-model="searchForm.userName"
              clearable
              placeholder="请输入用户名称"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="login-log-page__ip-item" label="登录 IP">
            <ElInput
              v-model="searchForm.ip"
              clearable
              placeholder="请输入登录 IP"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="login-log-page__date-item" label="记录日期">
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

      <ElCard class="login-log-page__table-card" shadow="never">
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
            row-key="loginLogId"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #loginResult="{ row }">
              <ElTag
                :type="resolveResultType(row.loginResult)"
                effect="plain"
                size="small"
              >
                {{ resolveResultLabel(row.loginResult) }}
              </ElTag>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.login-log-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.login-log-page__search-card,
.login-log-page__table-card {
  border-radius: 8px;
}

.login-log-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.login-log-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.login-log-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.login-log-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.login-log-page :deep(.art-table-panel),
.login-log-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.login-log-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.login-log-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.login-log-page__name-item :deep(.el-form-item__content),
.login-log-page__ip-item :deep(.el-form-item__content) {
  width: 220px;
}

.login-log-page__date-item :deep(.el-form-item__content) {
  width: 320px;
}

@media (width <= 768px) {
  .login-log-page__name-item :deep(.el-form-item__content),
  .login-log-page__ip-item :deep(.el-form-item__content),
  .login-log-page__date-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
