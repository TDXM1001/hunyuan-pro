<script setup lang="ts">
import type {
  ConfigAddForm,
  ConfigRecord,
  ConfigUpdateForm,
} from '#/api/system/config';
import type { ColumnOption } from '@vben/art-hooks/table';
import type { FormInstance, FormRules } from 'element-plus';

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
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElSpace,
} from 'element-plus';

import {
  addConfig,
  queryConfigPage,
  updateConfig,
} from '#/api/system/config';

defineOptions({ name: 'SystemSupportConfigList' });

interface ConfigFormModel extends ConfigAddForm {
  configId?: number;
}

const loading = ref(false);
const keyword = ref('');
const showSearchBar = ref(true);
const rows = ref<ConfigRecord[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const formRef = ref<FormInstance>();
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const formData = reactive<ConfigFormModel>({
  configKey: '',
  configName: '',
  configValue: '',
  remark: '',
});

const rules: FormRules<ConfigFormModel> = {
  configKey: [{ required: true, message: '请输入参数 Key', trigger: 'blur' }],
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入参数值', trigger: 'blur' }],
};

const columnsFactory = (): ColumnOption<ConfigRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'configKey', label: '参数 Key', minWidth: 220 },
  { prop: 'configName', label: '参数名称', minWidth: 180 },
  {
    prop: 'configValue',
    label: '参数值',
    minWidth: 260,
    formatter: (row) => row.configValue || '-',
  },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 200,
    formatter: (row) => row.remark || '-',
  },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 96,
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

function resetForm() {
  Object.assign(formData, {
    configKey: '',
    configName: '',
    configValue: '',
    remark: '',
  });
  formData.configId = undefined;
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryConfigPage({
      configKey: keyword.value,
      pageNum: pagination.current,
      pageSize: pagination.size,
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
  keyword.value = '';
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function openAddDialog() {
  dialogMode.value = 'add';
  resetForm();
  dialogVisible.value = true;
}

function openEditDialog(row: ConfigRecord) {
  dialogMode.value = 'edit';
  Object.assign(formData, {
    configId: row.configId,
    configKey: row.configKey,
    configName: row.configName,
    configValue: row.configValue,
    remark: row.remark || '',
  });
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  if (dialogMode.value === 'add') {
    await addConfig(formData as ConfigAddForm);
    ElMessage.success('新增参数配置成功');
  } else {
    await updateConfig(formData as ConfigUpdateForm);
    ElMessage.success('更新参数配置成功');
  }

  dialogVisible.value = false;
  await loadData();
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
    ElMessage.error(error?.message || '参数配置数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="config-page">
      <ElCard
        v-show="showSearchBar"
        class="config-page__search-card"
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
          <ElFormItem class="config-page__keyword-item" label="参数 Key">
            <ElInput
              v-model="keyword"
              clearable
              placeholder="请输入参数 Key"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="config-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="openAddDialog">
                新增参数
              </ElButton>
            </template>
          </ArtTableHeader>

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
            <template #actions="{ row }">
              <ElSpace class="config-page__actions">
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openEditDialog(row)"
                >
                  编辑
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <ElDialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增参数配置' : '编辑参数配置'"
      width="640px"
      @closed="resetForm"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
        <ElFormItem label="参数 Key" prop="configKey">
          <ElInput v-model="formData.configKey" placeholder="请输入参数 Key" />
        </ElFormItem>
        <ElFormItem label="参数名称" prop="configName">
          <ElInput v-model="formData.configName" placeholder="请输入参数名称" />
        </ElFormItem>
        <ElFormItem label="参数值" prop="configValue">
          <ElInput
            v-model="formData.configValue"
            :rows="4"
            placeholder="请输入参数值"
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="备注" prop="remark">
          <ElInput
            v-model="formData.remark"
            maxlength="255"
            placeholder="请输入备注"
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
  </Page>
</template>

<style scoped>
.config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.config-page__search-card,
.config-page__table-card {
  border-radius: 8px;
}

.config-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.config-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.config-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.config-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.config-page :deep(.art-table-panel),
.config-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.config-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.config-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.config-page__keyword-item :deep(.el-form-item__content) {
  width: 260px;
}

.config-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.config-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.config-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .config-page__keyword-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
