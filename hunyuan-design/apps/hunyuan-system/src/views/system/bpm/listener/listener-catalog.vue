<script setup lang="ts">
import type { BpmListenerRecord } from '#/api/system/bpm';
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

import { ElCard, ElFormItem, ElInput, ElMessage, ElOption, ElSelect, ElTag } from 'element-plus';

import { queryBpmListenerCatalog, queryBpmListenerChannelOptions } from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmListenerCatalog' });

interface ChannelOption {
  label: string;
  value: string;
}

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmListenerRecord[]>([]);
const channelOptions = ref<ChannelOption[]>([]);

const searchForm = reactive({
  channel: '',
  keyword: '',
});

const columnsFactory = (): ColumnOption<BpmListenerRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'listenerCode', label: '监听器编码', minWidth: 180 },
  { prop: 'listenerName', label: '监听器名称', minWidth: 180 },
  { prop: 'channels', label: '通知渠道', minWidth: 220, useSlot: true },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const filteredRows = computed(() => {
  const keyword = searchForm.keyword.trim().toLowerCase();
  return rows.value.filter((item) => {
    const matchesKeyword =
      !keyword ||
      [item.listenerCode, item.listenerName]
        .filter(Boolean)
        .some((field) => field.toLowerCase().includes(keyword));
    const matchesChannel =
      !searchForm.channel || item.channels.includes(searchForm.channel);
    return matchesKeyword && matchesChannel;
  });
});

async function loadData() {
  loading.value = true;
  try {
    const [listenerList, channelList] = await Promise.all([
      queryBpmListenerCatalog(),
      queryBpmListenerChannelOptions(),
    ]);
    rows.value = listenerList ?? [];
    channelOptions.value = channelList ?? [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  searchForm.keyword = searchForm.keyword.trim();
}

function handleReset() {
  Object.assign(searchForm, {
    channel: '',
    keyword: '',
  });
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

onMounted(() => {
  void loadData().catch((error) => {
    ElMessage.error(error?.message || '流程监听器目录加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="listener-page">
      <ElCard v-show="showSearchBar" class="listener-page__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="筛选"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem label="关键字">
            <ElInput
              v-model="searchForm.keyword"
              clearable
              placeholder="请输入监听器编码或名称"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="通知渠道">
            <ElSelect v-model="searchForm.channel" clearable placeholder="请选择通知渠道">
              <ElOption
                v-for="item in channelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="listener-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          />

          <ArtTable :columns="columns" :data="filteredRows" height="100%" :loading="loading">
            <template #channels="{ row }">
              <div class="listener-page__channels">
                <ElTag
                  v-for="channel in row.channels"
                  :key="channel"
                  effect="plain"
                  size="small"
                >
                  {{ channel }}
                </ElTag>
              </div>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.listener-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.listener-page__search-card,
.listener-page__table-card {
  border-radius: 8px;
}

.listener-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.listener-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.listener-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.listener-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.listener-page :deep(.art-table-panel),
.listener-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.listener-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.listener-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.listener-page__channels {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
