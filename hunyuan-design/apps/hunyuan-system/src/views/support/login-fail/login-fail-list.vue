<script setup lang="ts">
import type {
  LoginFailPageQueryParams,
  LoginFailRecord,
} from '#/api/system/network-protect';
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
  ElDatePicker,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

import {
  batchDeleteLoginFails,
  queryLoginFailPage,
} from '#/api/system/network-protect';

defineOptions({ name: 'SystemNetworkSecurityLoginFailList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<LoginFailRecord[]>([]);
const selectedRows = ref<LoginFailRecord[]>([]);

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchForm = reactive({
  lockDateRange: [] as string[],
  lockFlag: undefined as boolean | undefined,
  loginName: '',
});

const columnsFactory = (): ColumnOption<LoginFailRecord>[] => [
  { type: 'selection', width: 50, align: 'center' },
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'loginName', label: '登录名', minWidth: 180 },
  { prop: 'loginFailCount', label: '失败次数', width: 100, align: 'center' },
  { prop: 'lockFlag', label: '锁定状态', width: 100, align: 'center', useSlot: true },
  {
    prop: 'loginLockBeginTime',
    label: '锁定开始时间',
    minWidth: 180,
    formatter: (row) => row.loginLockBeginTime || '-',
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    minWidth: 180,
    formatter: (row) => row.updateTime || '-',
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function buildQueryParams(): LoginFailPageQueryParams {
  return {
    lockFlag: searchForm.lockFlag,
    loginLockBeginTimeBegin: searchForm.lockDateRange[0] || '',
    loginLockBeginTimeEnd: searchForm.lockDateRange[1] || '',
    loginName: searchForm.loginName,
    pageNum: pagination.current,
    pageSize: pagination.size,
  };
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryLoginFailPage(buildQueryParams());
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
    lockDateRange: [],
    lockFlag: undefined,
    loginName: '',
  });
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function handleSelectionChange(values: LoginFailRecord[]) {
  selectedRows.value = values;
}

async function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要清理的锁定记录');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定要清理选中的 ${selectedRows.value.length} 条登录失败记录吗？`,
      '批量清理确认',
      { type: 'warning' },
    );

    const loginFailIds = [...new Set(selectedRows.value.map((item) => item.loginFailId))];
    await batchDeleteLoginFails(loginFailIds);
    selectedRows.value = [];
    ElMessage.success('登录失败记录清理成功');
    await loadData();
  } catch {
    // 用户取消批量清理时不提示错误。
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
    ElMessage.error(error?.message || '登录失败记录加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="login-fail-page">
      <ElCard
        v-show="showSearchBar"
        class="login-fail-page__search-card"
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
          <ElFormItem class="login-fail-page__name-item" label="登录名">
            <ElInput
              v-model="searchForm.loginName"
              clearable
              placeholder="请输入登录名"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="login-fail-page__status-item" label="锁定状态">
            <ElSelect
              v-model="searchForm.lockFlag"
              clearable
              placeholder="请选择锁定状态"
            >
              <ElOption :value="true" label="已锁定" />
              <ElOption :value="false" label="未锁定" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem class="login-fail-page__date-item" label="锁定日期">
            <ElDatePicker
              v-model="searchForm.lockDateRange"
              end-placeholder="结束日期"
              range-separator="至"
              start-placeholder="开始日期"
              type="daterange"
              value-format="YYYY-MM-DD"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="login-fail-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElSpace>
                <ElButton
                  :disabled="selectedRows.length === 0"
                  type="primary"
                  @click="handleBatchDelete"
                >
                  批量清理
                </ElButton>
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
            row-key="loginFailId"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
            @selection-change="handleSelectionChange"
          >
            <template #lockFlag="{ row }">
              <ElTag
                :type="row.lockFlag ? 'danger' : 'info'"
                effect="plain"
                size="small"
              >
                {{ row.lockFlag ? '已锁定' : '未锁定' }}
              </ElTag>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.login-fail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.login-fail-page__search-card,
.login-fail-page__table-card {
  border-radius: 8px;
}

.login-fail-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.login-fail-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.login-fail-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.login-fail-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.login-fail-page :deep(.art-table-panel),
.login-fail-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.login-fail-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.login-fail-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.login-fail-page__name-item :deep(.el-form-item__content) {
  width: 220px;
}

.login-fail-page__status-item :deep(.el-form-item__content) {
  width: 168px;
}

.login-fail-page__date-item :deep(.el-form-item__content) {
  width: 320px;
}

@media (width <= 768px) {
  .login-fail-page__name-item :deep(.el-form-item__content),
  .login-fail-page__status-item :deep(.el-form-item__content),
  .login-fail-page__date-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
