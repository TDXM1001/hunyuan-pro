<script setup lang="ts">
import type { BpmStartableDefinitionRecord } from '#/api/system/bpm';
import type { ColumnOption } from '@vben/art-hooks/table';

import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  useTableColumns,
} from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';

import { ElButton, ElCard, ElMessage } from 'element-plus';

import { queryBpmStartableDefinitions } from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmRuntimeStartableList' });

const loading = ref(false);
const rows = ref<BpmStartableDefinitionRecord[]>([]);
const router = useRouter();

const columnsFactory = (): ColumnOption<BpmStartableDefinitionRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'definitionName', label: '流程名称', minWidth: 180 },
  { prop: 'definitionKey', label: '流程编码', minWidth: 160 },
  { prop: 'definitionVersion', label: '版本', width: 90, align: 'center' },
  {
    prop: 'categoryNameSnapshot',
    label: '分类',
    minWidth: 140,
    formatter: (row) => row.categoryNameSnapshot || '-',
  },
  {
    prop: 'formNameSnapshot',
    label: '表单',
    minWidth: 160,
    formatter: (row) => row.formNameSnapshot || '-',
  },
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

async function loadData() {
  loading.value = true;
  try {
    rows.value = (await queryBpmStartableDefinitions()).filter(
      (item) => item.definitionSource === 'GRAPH',
    );
  } finally {
    loading.value = false;
  }
}

function handleStart(row: BpmStartableDefinitionRecord) {
  const query = row.graphDefinitionVersionId
    ? { graphDefinitionVersionId: String(row.graphDefinitionVersionId) }
    : undefined;
  if (!query) {
    ElMessage.warning('流程定义版本无效，请刷新后重试');
    return;
  }
  void router.push({
    name: 'SystemBpmRuntimeStartFormRoute',
    query,
  });
}

onMounted(() => {
  void loadData().catch((error) => {
    ElMessage.error(error?.message || '可发起流程加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <ElCard class="runtime-startable-page" shadow="never">
      <ArtTablePanel>
        <ArtTableHeader
          v-model="columnChecks"
          :loading="loading"
          layout="size,fullscreen,columns,settings"
        />
        <ArtTable :columns="columns" :data="rows" height="100%" :loading="loading">
          <template #actions="{ row }">
            <div class="runtime-startable-page__actions">
              <ElButton link size="small" type="primary" @click="handleStart(row)">
                发起
              </ElButton>
            </div>
          </template>
        </ArtTable>
      </ArtTablePanel>
    </ElCard>
  </Page>
</template>

<style scoped>
.runtime-startable-page {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.runtime-startable-page :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.runtime-startable-page :deep(.art-table-panel),
.runtime-startable-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.runtime-startable-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.runtime-startable-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.runtime-startable-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}
</style>
