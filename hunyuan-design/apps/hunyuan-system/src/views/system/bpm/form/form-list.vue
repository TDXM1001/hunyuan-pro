<script setup lang="ts">
import type {
  BpmFormAddForm,
  BpmFormRecord,
  BpmFormUpdateForm,
} from '#/api/system/bpm';
import type { ColumnOption } from '@vben/art-hooks/table';
import type { FormInstance, FormRules } from 'element-plus';

import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

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
  ElOption,
  ElSelect,
  ElSpace,
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  addBpmForm,
  buildEmptyBpmFormDesignerSnapshot,
  getBpmFormDetail,
  queryBpmFormPage,
  updateBpmForm,
} from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmFormList' });

interface BpmFormFormModel extends BpmFormAddForm {
  formId?: number;
}

const router = useRouter();
const loading = ref(false);
const submitting = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmFormRecord[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const formRef = ref<FormInstance>();

const searchForm = reactive({
  disabledFlag: undefined as boolean | undefined,
  formKey: '',
  formName: '',
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const formData = reactive<BpmFormFormModel>({
  disabledFlag: false,
  formKey: '',
  formName: '',
  ...buildEmptyBpmFormDesignerSnapshot(),
  remark: '',
});

const rules: FormRules<BpmFormFormModel> = {
  formKey: [{ required: true, message: '请输入表单编码', trigger: 'blur' }],
  formName: [{ required: true, message: '请输入表单名称', trigger: 'blur' }],
};

const columnsFactory = (): ColumnOption<BpmFormRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'formKey', label: '表单编码', minWidth: 160 },
  { prop: 'formName', label: '表单名称', minWidth: 180 },
  { prop: 'disabledFlag', label: '状态', width: 100, align: 'center', useSlot: true },
  { prop: 'remark', label: '备注', minWidth: 220, formatter: (row) => row.remark || '-' },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 180,
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
    disabledFlag: false,
    formId: undefined,
    formKey: '',
    formName: '',
    ...buildEmptyBpmFormDesignerSnapshot(),
    remark: '',
  });
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryBpmFormPage({
      disabledFlag: searchForm.disabledFlag,
      formKey: searchForm.formKey,
      formName: searchForm.formName,
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
  Object.assign(searchForm, {
    disabledFlag: undefined,
    formKey: '',
    formName: '',
  });
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

async function openEditDialog(row: BpmFormRecord) {
  dialogMode.value = 'edit';
  const detail = await getBpmFormDetail(row.formId);
  Object.assign(formData, {
    disabledFlag: detail.disabledFlag ?? false,
    formId: detail.formId,
    formKey: detail.formKey,
    formName: detail.formName,
    layoutJson: detail.layoutJson || '',
    remark: detail.remark || '',
    schemaJson: detail.schemaJson || '',
  });
  dialogVisible.value = true;
}

function openDesigner(row: BpmFormRecord) {
  void router.push({
    path: '/system/bpm/form/designer',
    query: { formId: String(row.formId) },
  });
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    if (dialogMode.value === 'add') {
      await addBpmForm({
        ...formData,
        ...buildEmptyBpmFormDesignerSnapshot(),
      } as BpmFormAddForm);
      ElMessage.success('流程表单新增成功');
    } else {
      await updateBpmForm(formData as BpmFormUpdateForm);
      ElMessage.success('流程表单更新成功');
    }

    dialogVisible.value = false;
    await loadData();
  } finally {
    submitting.value = false;
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
    ElMessage.error(error?.message || '流程表单数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="form-page">
      <ElCard v-show="showSearchBar" class="form-page__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem label="表单编码">
            <ElInput
              v-model="searchForm.formKey"
              clearable
              placeholder="请输入表单编码"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="表单名称">
            <ElInput
              v-model="searchForm.formName"
              clearable
              placeholder="请输入表单名称"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="状态">
            <ElSelect v-model="searchForm.disabledFlag" clearable placeholder="请选择状态">
              <ElOption label="启用" :value="false" />
              <ElOption label="禁用" :value="true" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="form-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="openAddDialog">新增表单</ElButton>
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
            <template #disabledFlag="{ row }">
              <ElTag :type="row.disabledFlag ? 'danger' : 'success'" effect="plain" size="small">
                {{ row.disabledFlag ? '禁用' : '启用' }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <ElSpace class="form-page__actions">
                <ElButton link size="small" type="primary" @click="openEditDialog(row)">
                  编辑
                </ElButton>
                <ElButton link size="small" type="primary" @click="openDesigner(row)">
                  设计
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <ElDialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增流程表单' : '编辑流程表单'"
      width="640px"
      @closed="resetForm"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
        <div class="form-page__form-grid">
          <ElFormItem label="表单编码" prop="formKey">
            <ElInput v-model="formData.formKey" placeholder="请输入表单编码" />
          </ElFormItem>
          <ElFormItem label="表单名称" prop="formName">
            <ElInput v-model="formData.formName" placeholder="请输入表单名称" />
          </ElFormItem>
          <ElFormItem label="禁用状态" prop="disabledFlag">
            <ElSwitch v-model="formData.disabledFlag" />
          </ElFormItem>
          <ElFormItem class="form-page__form-span-2" label="备注" prop="remark">
            <ElInput
              v-model="formData.remark"
              maxlength="500"
              placeholder="请输入备注"
              show-word-limit
              type="textarea"
            />
          </ElFormItem>
        </div>
      </ElForm>

      <template #footer>
        <ElSpace>
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton :loading="submitting" type="primary" @click="handleSubmit">保存</ElButton>
        </ElSpace>
      </template>
    </ElDialog>
  </Page>
</template>

<style scoped>
.form-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.form-page__search-card,
.form-page__table-card {
  border-radius: 8px;
}

.form-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.form-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.form-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.form-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.form-page :deep(.art-table-panel),
.form-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.form-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.form-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.form-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.form-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.form-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.form-page__form-grid {
  display: grid;
  gap: 0 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.form-page__form-span-2 {
  grid-column: 1 / -1;
}

@media (width <= 768px) {
  .form-page__form-grid {
    grid-template-columns: 1fr;
  }

  .form-page__form-span-2 {
    grid-column: auto;
  }
}
</style>
