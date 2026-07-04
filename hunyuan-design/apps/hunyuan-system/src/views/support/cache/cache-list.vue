<script setup lang="ts">
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, onMounted, reactive, ref, watch } from 'vue';

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
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElSpace,
} from 'element-plus';

import { queryCacheNames, removeCache } from '#/api/system/cache';

import CacheKeyDrawerPanel from './components/cache-key-drawer.vue';

defineOptions({ name: 'SystemSupportCacheList' });

interface CacheRow {
  cacheName: string;
}

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<string[]>([]);
const selectedCacheName = ref('');

const [CacheKeyDrawer, cacheKeyDrawerApi] = useVbenDrawer({
  connectedComponent: CacheKeyDrawerPanel,
  destroyOnClose: false,
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const keyword = ref('');

const columnsFactory = (): ColumnOption<CacheRow>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'cacheName', label: 'cacheName', minWidth: 360 },
  {
    prop: 'actions',
    label: '操作',
    width: 160,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const filteredRows = computed(() => {
  const searchText = keyword.value.trim().toLowerCase();
  return rows.value
    .filter((item) => !searchText || item.toLowerCase().includes(searchText))
    .map((cacheName) => ({ cacheName }));
});

const displayRows = computed(() => {
  const start = (pagination.current - 1) * pagination.size;
  return filteredRows.value.slice(start, start + pagination.size);
});

async function loadData() {
  loading.value = true;
  try {
    rows.value = (await queryCacheNames()) ?? [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
}

function handleReset() {
  keyword.value = '';
  pagination.current = 1;
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function openCacheKeyDrawer(row: CacheRow) {
  selectedCacheName.value = row.cacheName;
  cacheKeyDrawerApi.open();
}

async function handleRemove(row: CacheRow) {
  try {
    await ElMessageBox.confirm(
      `确定要删除缓存“${row.cacheName}”吗？`,
      '删除确认',
      { type: 'warning' },
    );
    await removeCache(row.cacheName);
    ElMessage.success('缓存已删除');
    if (selectedCacheName.value === row.cacheName) {
      selectedCacheName.value = '';
      await cacheKeyDrawerApi.close();
    }
    await loadData();
  } catch {
    // 用户取消
  }
}

function handleCurrentChange(value: number) {
  pagination.current = value;
}

function handleSizeChange(value: number) {
  pagination.size = value;
  pagination.current = 1;
}

watch(
  filteredRows,
  (value) => {
    pagination.total = value.length;
    const maxPage = Math.max(1, Math.ceil(value.length / pagination.size));
    if (pagination.current > maxPage) {
      pagination.current = maxPage;
    }
  },
  { immediate: true },
);

onMounted(() => {
  void loadData();
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="cache-page">
      <ElCard
        v-show="showSearchBar"
        class="cache-page__search-card"
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
          <ElFormItem class="cache-page__keyword-item" label="缓存名称">
            <ElInput
              v-model="keyword"
              clearable
              placeholder="请输入 cacheName"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="cache-page__table-card" shadow="never">
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
            :data="displayRows"
            :height="'100%'"
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
            row-key="cacheName"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #actions="{ row }">
              <ElSpace class="cache-page__actions">
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openCacheKeyDrawer(row)"
                >
                  查看 Keys
                </ElButton>
                <ElButton
                  link
                  size="small"
                  type="danger"
                  @click="handleRemove(row)"
                >
                  删除缓存
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>

      <CacheKeyDrawer :cache-name="selectedCacheName" />
    </div>
  </Page>
</template>

<style scoped>
.cache-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.cache-page__search-card,
.cache-page__table-card {
  border-radius: 8px;
}

.cache-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.cache-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.cache-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.cache-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.cache-page :deep(.art-table-panel),
.cache-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.cache-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.cache-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.cache-page__keyword-item :deep(.el-form-item__content) {
  width: 220px;
}

.cache-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.cache-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.cache-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .cache-page__keyword-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
