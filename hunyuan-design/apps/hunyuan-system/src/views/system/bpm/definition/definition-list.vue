<script setup lang="ts">
import type { BpmDefinitionDetailRecord, BpmDefinitionRecord } from '#/api/system/bpm';
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
  ElDialog,
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
  enableBpmDefinitionStart,
  getBpmDefinitionDetail,
  queryBpmDefinitionPage,
  suspendBpmDefinitionStart,
} from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmDefinitionList' });

const loading = ref(false);
const detailLoading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmDefinitionRecord[]>([]);
const detailVisible = ref(false);
const detailData = ref<BpmDefinitionDetailRecord>();

const searchForm = reactive({
  definitionKey: '',
  definitionName: '',
  lifecycleState: undefined as number | undefined,
  startState: undefined as number | undefined,
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const columnsFactory = (): ColumnOption<BpmDefinitionRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'definitionKey', label: '定义编码', minWidth: 160 },
  { prop: 'definitionName', label: '定义名称', minWidth: 180 },
  { prop: 'definitionVersion', label: '版本', width: 90, align: 'center' },
  { prop: 'categoryNameSnapshot', label: '分类', minWidth: 120 },
  { prop: 'formNameSnapshot', label: '表单', minWidth: 140 },
  { prop: 'lifecycleState', label: '生命周期', width: 100, align: 'center', useSlot: true },
  { prop: 'startState', label: '发起状态', width: 100, align: 'center', useSlot: true },
  { prop: 'publishedByNameSnapshot', label: '发布人', width: 120, align: 'center' },
  { prop: 'publishedAt', label: '发布时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 150,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function getLifecycleLabel(value?: null | number) {
  return value === 1 ? '当前版本' : '历史版本';
}

function getLifecycleType(value?: null | number) {
  return value === 1 ? 'success' : 'info';
}

function getStartStateLabel(value?: null | number) {
  return value === 1 ? '可发起' : '停用';
}

function getStartStateType(value?: null | number) {
  return value === 1 ? 'success' : 'warning';
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryBpmDefinitionPage({
      definitionKey: searchForm.definitionKey,
      definitionName: searchForm.definitionName,
      lifecycleState: searchForm.lifecycleState,
      pageNum: pagination.current,
      pageSize: pagination.size,
      startState: searchForm.startState,
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
  Object.assign(searchForm, {
    definitionKey: '',
    definitionName: '',
    lifecycleState: undefined,
    startState: undefined,
  });
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

async function openDetailDialog(row: BpmDefinitionRecord) {
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    detailData.value = await getBpmDefinitionDetail(row.definitionId);
  } finally {
    detailLoading.value = false;
  }
}

async function handleSuspendStart(row: BpmDefinitionRecord) {
  try {
    await ElMessageBox.confirm(
      `确定停用流程定义“${row.definitionName}”的发起入口吗？`,
      '停用发起确认',
      { type: 'warning' },
    );
    await suspendBpmDefinitionStart(row.definitionId);
    ElMessage.success('已停用发起入口');
    await loadData();
  } catch {
    // 用户取消
  }
}

async function handleEnableStart(row: BpmDefinitionRecord) {
  try {
    await ElMessageBox.confirm(
      `确定启用流程定义“${row.definitionName}”的发起入口吗？`,
      '启用发起确认',
      { type: 'warning' },
    );
    await enableBpmDefinitionStart(row.definitionId);
    ElMessage.success('已启用发起入口');
    await loadData();
  } catch {
    // 用户取消
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
    ElMessage.error(error?.message || '流程定义数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="definition-page">
      <ElCard v-show="showSearchBar" class="definition-page__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem label="定义编码">
            <ElInput
              v-model="searchForm.definitionKey"
              clearable
              placeholder="请输入定义编码"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="定义名称">
            <ElInput
              v-model="searchForm.definitionName"
              clearable
              placeholder="请输入定义名称"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="生命周期">
            <ElSelect
              v-model="searchForm.lifecycleState"
              clearable
              placeholder="请选择生命周期"
            >
              <ElOption label="历史版本" :value="0" />
              <ElOption label="当前版本" :value="1" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="发起状态">
            <ElSelect v-model="searchForm.startState" clearable placeholder="请选择发起状态">
              <ElOption label="停用" :value="0" />
              <ElOption label="可发起" :value="1" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="definition-page__table-card" shadow="never">
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
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #lifecycleState="{ row }">
              <ElTag :type="getLifecycleType(row.lifecycleState)" effect="plain" size="small">
                {{ getLifecycleLabel(row.lifecycleState) }}
              </ElTag>
            </template>

            <template #startState="{ row }">
              <ElTag :type="getStartStateType(row.startState)" effect="plain" size="small">
                {{ getStartStateLabel(row.startState) }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <ElSpace class="definition-page__actions">
                <ElButton link size="small" type="primary" @click="openDetailDialog(row)">
                  详情
                </ElButton>
                <ElButton
                  v-if="row.startState === 1"
                  link
                  size="small"
                  type="warning"
                  @click="handleSuspendStart(row)"
                >
                  停用
                </ElButton>
                <ElButton
                  v-else
                  link
                  size="small"
                  type="success"
                  @click="handleEnableStart(row)"
                >
                  启用
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <ElDialog v-model="detailVisible" title="流程定义详情" width="920px">
      <div v-loading="detailLoading" class="definition-page__detail">
        <template v-if="detailData">
          <div class="definition-page__detail-grid">
            <div>
              <label>定义编码</label>
              <p>{{ detailData.definitionKey }}</p>
            </div>
            <div>
              <label>定义名称</label>
              <p>{{ detailData.definitionName }}</p>
            </div>
            <div>
              <label>定义版本</label>
              <p>{{ detailData.definitionVersion }}</p>
            </div>
            <div>
              <label>Flowable 定义 ID</label>
              <p>{{ detailData.engineProcessDefinitionId || '-' }}</p>
            </div>
          </div>
          <ElInput
            class="definition-page__detail-textarea"
            :model-value="detailData.compiledBpmnXml || ''"
            :rows="10"
            readonly
            type="textarea"
          />
          <ElInput
            class="definition-page__detail-textarea"
            :model-value="detailData.simpleModelSnapshotJson || ''"
            :rows="8"
            readonly
            type="textarea"
          />
          <ElInput
            class="definition-page__detail-textarea"
            :model-value="detailData.variableMappingSnapshotJson || ''"
            :rows="6"
            readonly
            type="textarea"
          />
        </template>
      </div>
    </ElDialog>
  </Page>
</template>

<style scoped>
.definition-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.definition-page__search-card,
.definition-page__table-card {
  border-radius: 8px;
}

.definition-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.definition-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.definition-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.definition-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.definition-page :deep(.art-table-panel),
.definition-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.definition-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.definition-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.definition-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.definition-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.definition-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.definition-page__detail {
  min-height: 240px;
}

.definition-page__detail-grid {
  display: grid;
  gap: 12px 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 16px;
}

.definition-page__detail-grid label {
  color: var(--el-text-color-secondary);
  display: block;
  font-size: 12px;
  margin-bottom: 4px;
}

.definition-page__detail-grid p {
  color: var(--el-text-color-primary);
  margin: 0;
  word-break: break-all;
}

.definition-page__detail-textarea + .definition-page__detail-textarea {
  margin-top: 12px;
}

@media (width <= 768px) {
  .definition-page__detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
