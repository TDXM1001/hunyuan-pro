<script setup lang="ts">
import type {
  ReloadItemRecord,
  ReloadResultRecord,
} from '#/api/system/reload';
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

import {
  ElCard,
  ElFormItem,
  ElInput,
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

import { queryReloadResults } from '#/api/system/reload';

defineOptions({ name: 'SystemSupportReloadResultDrawer' });

const props = defineProps<{
  reloadItem?: ReloadItemRecord;
}>();

const [Drawer, drawerApi] = useVbenDrawer();
const drawerOpen = drawerApi.useStore((state) => Boolean(state.isOpen));

const loading = ref(false);
const rows = ref<ReloadResultRecord[]>([]);
const keyword = ref('');
const resultFilter = ref<boolean>();
const lastLoadedTag = ref('');

const columnsFactory = (): ColumnOption<ReloadResultRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  {
    prop: 'result',
    label: 'result',
    width: 90,
    align: 'center',
    useSlot: true,
  },
  {
    prop: 'args',
    label: 'args',
    minWidth: 220,
    formatter: (row) => row.args || '-',
  },
  {
    prop: 'exception',
    label: 'exception',
    minWidth: 260,
    formatter: (row) => row.exception || '-',
  },
  {
    prop: 'createTime',
    label: '创建时间',
    minWidth: 180,
    formatter: (row) => row.createTime || '-',
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const drawerTitle = computed(() =>
  props.reloadItem?.tag ? `${props.reloadItem.tag} - 结果历史` : '结果历史',
);
const displayRows = computed(() => {
  const searchText = keyword.value.trim().toLowerCase();
  return rows.value.filter((item) => {
    const matchKeyword
      = !searchText
      || [item.args || '', item.exception || '', item.tag]
        .join(' ')
        .toLowerCase()
        .includes(searchText);
    const matchResult
      = resultFilter.value == null || item.result === resultFilter.value;

    return matchKeyword && matchResult;
  });
});

function resetFilters() {
  keyword.value = '';
  resultFilter.value = undefined;
}

async function loadData() {
  if (!props.reloadItem?.tag) {
    rows.value = [];
    return;
  }

  loading.value = true;
  try {
    rows.value = (await queryReloadResults(props.reloadItem.tag)) ?? [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  // 结果历史采用前端快速筛选，避免为只读追踪面额外扩展后端查询参数。
}

function handleReset() {
  resetFilters();
}

watch(
  () => [props.reloadItem?.tag, drawerOpen.value] as const,
  ([tag, isOpen]) => {
    if (!isOpen) {
      return;
    }

    if (!tag) {
      rows.value = [];
      return;
    }

    if (lastLoadedTag.value !== tag) {
      lastLoadedTag.value = tag;
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
    <div class="reload-result-drawer">
      <ElCard class="reload-result-drawer__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="筛选"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem class="reload-result-drawer__keyword-item" label="关键字">
            <ElInput
              v-model="keyword"
              clearable
              placeholder="请输入 args 或 exception"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem class="reload-result-drawer__result-item" label="结果">
            <ElSelect
              v-model="resultFilter"
              clearable
              placeholder="请选择结果"
            >
              <ElOption :value="true" label="成功" />
              <ElOption :value="false" label="失败" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="reload-result-drawer__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="size,fullscreen,columns,settings"
          >
            <template #left>
              <ElSpace class="reload-result-drawer__meta">
                <span>tag：{{ props.reloadItem?.tag || '-' }}</span>
                <span>identification：{{ props.reloadItem?.identification || '-' }}</span>
              </ElSpace>
            </template>
          </ArtTableHeader>

          <div class="reload-result-drawer__table-wrap">
            <ArtTable
              :columns="columns"
              :data="displayRows"
              height="100%"
              :loading="loading"
              row-key="createTime"
            >
              <template #result="{ row }">
                <ElTag
                  effect="plain"
                  size="small"
                  :type="row.result ? 'success' : 'danger'"
                >
                  {{ row.result ? '成功' : '失败' }}
                </ElTag>
              </template>
            </ArtTable>
          </div>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Drawer>
</template>

<style scoped>
.reload-result-drawer {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.reload-result-drawer__search-card,
.reload-result-drawer__table-card {
  border-radius: 8px;
}

.reload-result-drawer__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.reload-result-drawer__search-card :deep(.el-card__body) {
  padding: 16px;
}

.reload-result-drawer__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.reload-result-drawer__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.reload-result-drawer :deep(.art-table-panel) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.reload-result-drawer :deep(.art-table-header) {
  margin-bottom: 18px;
}

.reload-result-drawer :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.reload-result-drawer__table-wrap {
  flex: 1;
  min-height: 0;
}

.reload-result-drawer__meta {
  color: var(--el-text-color-regular);
  flex-wrap: wrap;
  font-size: 14px;
  line-height: 22px;
}

.reload-result-drawer__keyword-item :deep(.el-form-item__content) {
  width: 320px;
}

.reload-result-drawer__result-item :deep(.el-form-item__content) {
  width: 168px;
}

@media (width <= 768px) {
  .reload-result-drawer__keyword-item :deep(.el-form-item__content),
  .reload-result-drawer__result-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
