<script setup lang="ts">
import type {
  SerialNumberDefinition,
  SerialNumberRecord,
} from '#/api/system/serial-number';
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
  ElSpace,
} from 'element-plus';

import { querySerialNumberRecords } from '#/api/system/serial-number';

defineOptions({ name: 'SystemSupportSerialNumberRecordDrawer' });

const props = defineProps<{
  serialNumber?: SerialNumberDefinition;
}>();

const [Drawer, drawerApi] = useVbenDrawer();
const drawerOpen = drawerApi.useStore((state) => Boolean(state.isOpen));

const loading = ref(false);
const rows = ref<SerialNumberRecord[]>([]);
const total = ref(0);
const keyword = ref('');
const lastLoadedSerialNumberId = ref<null | number>(null);

const columnsFactory = (): ColumnOption<SerialNumberRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'recordDate', label: '记录日期', minWidth: 160 },
  {
    prop: 'lastNumber',
    label: 'lastNumber',
    minWidth: 140,
    formatter: (row) => (row.lastNumber == null ? '-' : `${row.lastNumber}`),
  },
  {
    prop: 'count',
    label: 'count',
    minWidth: 120,
    formatter: (row) => (row.count == null ? '-' : `${row.count}`),
  },
  {
    prop: 'lastTime',
    label: '最后生成时间',
    minWidth: 180,
    formatter: (row) => row.lastTime || '-',
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    minWidth: 180,
    formatter: (row) => row.updateTime || '-',
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const drawerTitle = computed(() =>
  props.serialNumber?.businessName
    ? `${props.serialNumber.businessName} - 生成记录`
    : '生成记录',
);
const filteredRows = computed(() => {
  const searchText = keyword.value.trim().toLowerCase();
  if (!searchText) {
    return rows.value;
  }
  return rows.value.filter((item) =>
    [item.recordDate, item.lastNumber, item.count, item.lastTime]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
      .includes(searchText),
  );
});

function resetFilters() {
  keyword.value = '';
}

async function loadData() {
  if (!props.serialNumber?.serialNumberId) {
    rows.value = [];
    total.value = 0;
    return;
  }

  loading.value = true;
  try {
    const result = await querySerialNumberRecords({
      pageNum: 1,
      pageSize: 200,
      serialNumberId: props.serialNumber.serialNumberId,
    });
    rows.value = result?.list ?? [];
    total.value = result?.total ?? rows.value.length;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  // 当前抽屉使用已加载记录做快速筛选，避免为了一个只读台账再造额外查询参数。
}

function handleReset() {
  resetFilters();
}

watch(
  () => [props.serialNumber?.serialNumberId, drawerOpen.value] as const,
  ([serialNumberId, isOpen]) => {
    if (!isOpen) {
      return;
    }

    if (!serialNumberId) {
      rows.value = [];
      total.value = 0;
      return;
    }

    if (lastLoadedSerialNumberId.value !== serialNumberId) {
      lastLoadedSerialNumberId.value = serialNumberId;
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
    <div class="serial-number-record-drawer">
      <ElCard class="serial-number-record-drawer__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="筛选"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem
            class="serial-number-record-drawer__keyword-item"
            label="记录筛选"
          >
            <ElInput
              v-model="keyword"
              clearable
              placeholder="按 recordDate、lastNumber、count 筛选"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="serial-number-record-drawer__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="size,fullscreen,columns,settings"
          >
            <template #left>
              <ElSpace class="serial-number-record-drawer__meta">
                <span>serialNumberId：{{ props.serialNumber?.serialNumberId || '-' }}</span>
                <span>businessName：{{ props.serialNumber?.businessName || '-' }}</span>
              </ElSpace>
            </template>
          </ArtTableHeader>

          <div class="serial-number-record-drawer__table-wrap">
            <ArtTable
              :columns="columns"
              :data="filteredRows"
              height="100%"
              :loading="loading"
              row-key="recordDate"
            />
          </div>

          <div class="serial-number-record-drawer__summary">
            当前展示 {{ filteredRows.length }} 条，共 {{ total }} 条
          </div>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Drawer>
</template>

<style scoped>
.serial-number-record-drawer {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.serial-number-record-drawer__search-card,
.serial-number-record-drawer__table-card {
  border-radius: 8px;
}

.serial-number-record-drawer__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.serial-number-record-drawer__search-card :deep(.el-card__body) {
  padding: 16px;
}

.serial-number-record-drawer__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.serial-number-record-drawer__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.serial-number-record-drawer :deep(.art-table-panel) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.serial-number-record-drawer :deep(.art-table-header) {
  margin-bottom: 18px;
}

.serial-number-record-drawer :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.serial-number-record-drawer__table-wrap {
  flex: 1;
  min-height: 0;
}

.serial-number-record-drawer__meta {
  color: var(--el-text-color-regular);
  flex-wrap: wrap;
  font-size: 14px;
  line-height: 22px;
}

.serial-number-record-drawer__summary {
  color: var(--el-text-color-regular);
  flex-shrink: 0;
  font-size: 14px;
  line-height: 22px;
  padding-top: 12px;
  text-align: right;
}

.serial-number-record-drawer__keyword-item :deep(.el-form-item__content) {
  width: 360px;
}

@media (width <= 768px) {
  .serial-number-record-drawer__keyword-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
