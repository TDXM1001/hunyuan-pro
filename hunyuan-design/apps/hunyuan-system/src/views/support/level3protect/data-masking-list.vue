<script setup lang="ts">
import type { DataMaskingDemoRecord } from '#/api/system/data-masking';
import type { ColumnOption } from '@vben/art-hooks/table';

import { onMounted, ref } from 'vue';

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
} from 'element-plus';

import { queryDataMaskingDemoList } from '#/api/system/data-masking';

defineOptions({ name: 'SystemNetworkSecurityDataMaskingList' });

const loading = ref(false);
const rows = ref<DataMaskingDemoRecord[]>([]);

const columnsFactory = (): ColumnOption<DataMaskingDemoRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  {
    prop: 'userId',
    label: '用户 ID',
    minWidth: 140,
    formatter: (row) => row.userId || '-',
  },
  {
    prop: 'phone',
    label: '手机号',
    minWidth: 140,
    formatter: (row) => row.phone || '-',
  },
  {
    prop: 'idCard',
    label: '身份证号',
    minWidth: 180,
    formatter: (row) => row.idCard || '-',
  },
  {
    prop: 'address',
    label: '地址',
    minWidth: 220,
    formatter: (row) => row.address || '-',
  },
  {
    prop: 'email',
    label: '邮箱',
    minWidth: 180,
    formatter: (row) => row.email || '-',
  },
  {
    prop: 'bankCard',
    label: '银行卡',
    minWidth: 200,
    formatter: (row) => row.bankCard || '-',
  },
  {
    prop: 'carLicense',
    label: '车牌号',
    minWidth: 160,
    formatter: (row) => row.carLicense || '-',
  },
  {
    prop: 'other',
    label: '其他字段',
    minWidth: 160,
    formatter: (row) => row.other || '-',
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

async function loadData() {
  loading.value = true;
  try {
    rows.value = (await queryDataMaskingDemoList()) ?? [];
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="data-masking-page">
      <ElCard class="data-masking-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="fullscreen,columns,settings"
          >
            <template #left>
              <ElButton type="primary" @click="loadData">刷新演示数据</ElButton>
            </template>
          </ArtTableHeader>

          <ArtTable
            :columns="columns"
            :data="rows"
            height="100%"
            :loading="loading"
            row-key="userId"
          />
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.data-masking-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.data-masking-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.data-masking-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.data-masking-page :deep(.art-table-panel),
.data-masking-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.data-masking-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.data-masking-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}
</style>
