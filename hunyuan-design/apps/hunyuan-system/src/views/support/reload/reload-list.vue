<script setup lang="ts">
import type { ReloadFormModel, ReloadItemRecord } from '#/api/system/reload';
import type { ColumnOption } from '@vben/art-hooks/table';
import type { FormInstance, FormRules } from 'element-plus';

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
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElSpace,
} from 'element-plus';

import { queryReloadItems, updateReloadItem } from '#/api/system/reload';

import ReloadResultDrawerPanel from './components/reload-result-drawer.vue';

defineOptions({ name: 'SystemSupportReloadList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<ReloadItemRecord[]>([]);
const selectedReloadItem = ref<ReloadItemRecord>();
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();

const [ReloadResultDrawer, reloadResultDrawerApi] = useVbenDrawer({
  connectedComponent: ReloadResultDrawerPanel,
  destroyOnClose: false,
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const searchKeyword = ref('');

const formData = reactive<ReloadFormModel>({
  args: '',
  identification: '',
  tag: '',
});

const rules: FormRules<ReloadFormModel> = {
  identification: [
    { required: true, message: '请输入状态标识', trigger: 'blur' },
  ],
  tag: [{ required: true, message: '标签不能为空', trigger: 'blur' }],
};

const columnsFactory = (): ColumnOption<ReloadItemRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'tag', label: 'tag', minWidth: 180 },
  { prop: 'identification', label: 'identification', minWidth: 180 },
  {
    prop: 'args',
    label: 'args',
    minWidth: 220,
    formatter: (row) => row.args || '-',
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    minWidth: 180,
    formatter: (row) => row.updateTime || '-',
  },
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
  const keyword = searchKeyword.value.trim().toLowerCase();
  return rows.value.filter(
    (item) =>
      !keyword
      || [item.tag, item.identification, item.args || '']
        .join(' ')
        .toLowerCase()
        .includes(keyword),
  );
});

const displayRows = computed(() => {
  const start = (pagination.current - 1) * pagination.size;
  return filteredRows.value.slice(start, start + pagination.size);
});

function resetForm() {
  Object.assign(formData, {
    args: '',
    identification: '',
    tag: '',
  });
}

async function loadData() {
  loading.value = true;
  try {
    rows.value = (await queryReloadItems()) ?? [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
}

function handleReset() {
  searchKeyword.value = '';
  pagination.current = 1;
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function openUpdateDialog(row: ReloadItemRecord) {
  Object.assign(formData, {
    args: row.args || '',
    identification: row.identification,
    tag: row.tag,
  });
  dialogVisible.value = true;
}

function openResultDrawer(row: ReloadItemRecord) {
  selectedReloadItem.value = row;
  reloadResultDrawerApi.open();
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  await updateReloadItem(formData);
  dialogVisible.value = false;
  ElMessage.success('更新配置成功');
  await loadData();
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
    <div class="reload-page">
      <ElCard
        v-show="showSearchBar"
        class="reload-page__search-card"
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
          <ElFormItem class="reload-page__keyword-item" label="关键字">
            <ElInput
              v-model="searchKeyword"
              clearable
              placeholder="请输入 tag、identification 或 args"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="reload-page__table-card" shadow="never">
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
            row-key="tag"
            @pagination:current-change="handleCurrentChange"
            @pagination:size-change="handleSizeChange"
          >
            <template #actions="{ row }">
              <ElSpace class="reload-page__actions">
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openUpdateDialog(row)"
                >
                  更新配置
                </ElButton>
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openResultDrawer(row)"
                >
                  结果历史
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>

      <ReloadResultDrawer :reload-item="selectedReloadItem" />

      <ElDialog
        v-model="dialogVisible"
        title="更新配置"
        width="560px"
        @closed="resetForm"
      >
        <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
          <ElFormItem label="tag" prop="tag">
            <ElInput v-model="formData.tag" disabled />
          </ElFormItem>
          <ElFormItem label="identification" prop="identification">
            <ElInput
              v-model="formData.identification"
              placeholder="请输入新的运行标识"
            />
          </ElFormItem>
          <ElFormItem label="args" prop="args">
            <ElInput
              v-model="formData.args"
              :rows="4"
              placeholder="请输入参数"
              type="textarea"
            />
          </ElFormItem>
        </ElForm>

        <template #footer>
          <ElSpace>
            <ElButton @click="dialogVisible = false">取消</ElButton>
            <ElButton type="primary" @click="handleSubmit">保存</ElButton>
          </ElSpace>
        </template>
      </ElDialog>
    </div>
  </Page>
</template>

<style scoped>
.reload-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.reload-page__search-card,
.reload-page__table-card {
  border-radius: 8px;
}

.reload-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.reload-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.reload-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.reload-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.reload-page :deep(.art-table-panel),
.reload-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.reload-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.reload-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.reload-page__keyword-item :deep(.el-form-item__content) {
  width: 320px;
}

.reload-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.reload-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.reload-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .reload-page__keyword-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
