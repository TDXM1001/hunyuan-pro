<script setup lang="ts">
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, ref, watch } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  useTableColumns,
} from '@vben/art-hooks/table';
import { useVbenDrawer } from '@vben/common-ui';

import { ElCard, ElFormItem, ElInput, ElSpace } from 'element-plus';

import { queryCacheKeys } from '#/api/system/cache';

defineOptions({ name: 'SystemSupportCacheKeyDrawer' });

const props = defineProps<{
  cacheName?: string;
}>();

const [Drawer, drawerApi] = useVbenDrawer();
const drawerOpen = drawerApi.useStore((state) => Boolean(state.isOpen));

const loading = ref(false);
const rows = ref<string[]>([]);
const keyword = ref('');
const lastLoadedCacheName = ref('');

const columnsFactory = (): ColumnOption<{ cacheKey: string }>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'cacheKey', label: 'cacheKey', minWidth: 520 },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const drawerTitle = computed(() =>
  props.cacheName ? `${props.cacheName} - Keys` : '缓存 Keys',
);
const displayRows = computed(() => {
  const searchText = keyword.value.trim().toLowerCase();
  return rows.value
    .filter((item) => !searchText || item.toLowerCase().includes(searchText))
    .map((cacheKey) => ({ cacheKey }));
});

function resetFilters() {
  keyword.value = '';
}

async function loadData() {
  if (!props.cacheName) {
    rows.value = [];
    return;
  }

  loading.value = true;
  try {
    rows.value = (await queryCacheKeys(props.cacheName)) ?? [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  // 当前抽屉只做前端 key 快速筛选，避免把简单查看面做成额外复杂查询。
}

function handleReset() {
  resetFilters();
}

watch(
  () => [props.cacheName, drawerOpen.value] as const,
  ([cacheName, isOpen]) => {
    if (!isOpen) {
      return;
    }

    if (!cacheName) {
      rows.value = [];
      return;
    }

    if (lastLoadedCacheName.value !== cacheName) {
      lastLoadedCacheName.value = cacheName;
      resetFilters();
    }

    void loadData();
  },
  { immediate: true },
);
</script>

<template>
  <Drawer
    class="w-[1180px] max-w-[calc(100vw-24px)]"
    close-icon-placement="left"
    content-class="!p-0"
    :footer="false"
    :title="drawerTitle"
  >
    <div class="cache-key-drawer">
      <ElCard class="cache-key-drawer__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="筛选"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem class="cache-key-drawer__keyword-item" label="Key 筛选">
            <ElInput
              v-model="keyword"
              clearable
              placeholder="请输入 cacheKey"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="cache-key-drawer__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="size,fullscreen,columns,settings"
          >
            <template #left>
              <ElSpace class="cache-key-drawer__meta">
                <span>cacheName：{{ props.cacheName || '-' }}</span>
                <span>Key 数：{{ displayRows.length }}</span>
              </ElSpace>
            </template>
          </ArtTableHeader>

          <div class="cache-key-drawer__table-wrap">
            <ArtTable
              :columns="columns"
              :data="displayRows"
              height="100%"
              :loading="loading"
              row-key="cacheKey"
            />
          </div>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Drawer>
</template>

<style scoped>
.cache-key-drawer {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.cache-key-drawer__search-card,
.cache-key-drawer__table-card {
  border-radius: 8px;
}

.cache-key-drawer__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.cache-key-drawer__search-card :deep(.el-card__body) {
  padding: 16px;
}

.cache-key-drawer__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.cache-key-drawer__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.cache-key-drawer :deep(.art-table-panel) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.cache-key-drawer :deep(.art-table-header) {
  margin-bottom: 18px;
}

.cache-key-drawer :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.cache-key-drawer__table-wrap {
  flex: 1;
  min-height: 0;
}

.cache-key-drawer__meta {
  color: var(--el-text-color-regular);
  flex-wrap: wrap;
  font-size: 14px;
  line-height: 22px;
}

.cache-key-drawer__keyword-item :deep(.el-form-item__content) {
  width: 360px;
}

@media (width <= 768px) {
  .cache-key-drawer__keyword-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
