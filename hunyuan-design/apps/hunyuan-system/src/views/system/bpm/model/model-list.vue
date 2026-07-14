<script setup lang="ts">
import type { BpmCategoryRecord, BpmGraphDraftRecord } from '#/api/system/bpm';
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import { ArtTable, ArtTableHeader, ArtTablePanel, useTableColumns } from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';
import {
  ElButton,
  ElCard,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

import {
  publishBpmGraphDefinition,
  queryBpmCategoryPage,
  queryBpmGraphDraftPage,
} from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmModelList' });

const router = useRouter();
const loading = ref(false);
const publishingId = ref<number>();
const showSearchBar = ref(true);
const rows = ref<BpmGraphDraftRecord[]>([]);
const categoryOptions = ref<BpmCategoryRecord[]>([]);
const searchForm = reactive({
  categoryId: undefined as number | undefined,
  processKey: '',
  processName: '',
});
const pagination = reactive({ current: 1, size: 10, total: 0 });

const categoryNames = computed(() => new Map(
  categoryOptions.value.map((item) => [item.categoryId, item.categoryName]),
));
const columnsFactory = (): ColumnOption<BpmGraphDraftRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'processKey', label: '流程编码', minWidth: 180 },
  { prop: 'processName', label: '流程名称', minWidth: 180 },
  {
    prop: 'categoryId', label: '分类', minWidth: 140,
    formatter: (row) => categoryNames.value.get(row.categoryId || 0) || '-',
  },
  { prop: 'revision', label: '草稿修订', width: 100, align: 'center' },
  { prop: 'draftStatus', label: '状态', width: 100, align: 'center', useSlot: true },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
  { prop: 'actions', label: '操作', width: 170, align: 'center', fixed: 'right', useSlot: true },
];
const { columns, columnChecks } = useTableColumns(columnsFactory);
const tableHeight = computed(() => pagination.total > pagination.size ? 'calc(100% - 44px)' : '100%');

async function loadData() {
  loading.value = true;
  try {
    const result = await queryBpmGraphDraftPage({
      categoryId: searchForm.categoryId,
      pageNum: pagination.current,
      pageSize: pagination.size,
      processKey: searchForm.processKey,
      processName: searchForm.processName,
    });
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
  Object.assign(searchForm, { categoryId: undefined, processKey: '', processName: '' });
  handleSearch();
}

function openEditor(row?: BpmGraphDraftRecord) {
  void router.push({
    path: '/system/bpm/model/designer',
    query: row ? { draftId: String(row.draftId) } : {},
  });
}

async function publishDraft(row: BpmGraphDraftRecord) {
  publishingId.value = row.draftId;
  try {
    await publishBpmGraphDefinition(row.draftId);
    ElMessage.success('Graph 流程定义发布成功');
    await loadData();
  } finally {
    publishingId.value = undefined;
  }
}

onMounted(() => {
  void Promise.all([
    queryBpmCategoryPage({ pageNum: 1, pageSize: 200 }).then((page) => {
      categoryOptions.value = page?.list ?? [];
    }),
    loadData(),
  ]).catch((error) => ElMessage.error(error?.message || 'Graph 草稿加载失败'));
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="graph-list-page">
      <ElCard v-show="showSearchBar" class="graph-list-page__search" shadow="never">
        <ArtSearchPanel :collapsible="false" :loading="loading" :show-refresh="false"
          @reset="handleReset" @search="handleSearch">
          <ElFormItem label="流程编码">
            <ElInput v-model="searchForm.processKey" clearable placeholder="请输入流程编码" @keyup.enter="handleSearch" />
          </ElFormItem>
          <ElFormItem label="流程名称">
            <ElInput v-model="searchForm.processName" clearable placeholder="请输入流程名称" @keyup.enter="handleSearch" />
          </ElFormItem>
          <ElFormItem label="分类">
            <ElSelect v-model="searchForm.categoryId" clearable filterable placeholder="请选择分类">
              <ElOption v-for="item in categoryOptions" :key="item.categoryId"
                :label="item.categoryName" :value="item.categoryId" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="graph-list-page__table" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader v-model="columnChecks" :loading="loading"
            layout="search,size,fullscreen,columns,settings" :show-search-bar="showSearchBar"
            @search="showSearchBar = !showSearchBar">
            <template #left>
              <ElButton :icon="Plus" type="primary" @click="openEditor()">新建流程</ElButton>
            </template>
          </ArtTableHeader>
          <ArtTable :columns="columns" :data="rows" :height="tableHeight" :loading="loading"
            :pagination="pagination" :pagination-options="{
              align: 'center', hideOnSinglePage: false,
              layout: 'sizes, prev, pager, next, jumper', pageSizes: [10, 20, 30],
              showTotalSummary: true, size: 'small',
            }"
            @pagination:current-change="(value) => { pagination.current = value; loadData(); }"
            @pagination:size-change="(value) => { pagination.size = value; pagination.current = 1; loadData(); }">
            <template #draftStatus="{ row }">
              <ElTag effect="plain" type="warning">{{ row.draftStatus || 'DRAFT' }}</ElTag>
            </template>
            <template #actions="{ row }">
              <ElSpace>
                <ElButton link type="primary" @click="openEditor(row)">设计</ElButton>
                <ElButton :loading="publishingId === row.draftId" link type="success" @click="publishDraft(row)">发布</ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.graph-list-page { display: flex; flex-direction: column; gap: 16px; height: 100%; min-height: 0; }
.graph-list-page__search { border: 0; flex-shrink: 0; }
.graph-list-page__table { flex: 1; min-height: 0; overflow: hidden; }
.graph-list-page__table :deep(.el-card__body),
.graph-list-page :deep(.art-table-panel),
.graph-list-page :deep(.art-table) { height: 100%; min-height: 0; }
</style>
