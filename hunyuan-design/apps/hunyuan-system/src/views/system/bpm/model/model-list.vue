<script setup lang="ts">
import type {
  BpmCategoryRecord,
  BpmFormRecord,
  BpmModelAddForm,
  BpmModelRecord,
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
  ElInputNumber,
  ElMessage,
  ElOption,
  ElSelect,
  ElSpace,
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  addBpmModel,
  getBpmModelDetail,
  publishBpmDefinition,
  queryBpmCategoryPage,
  queryBpmFormPage,
  queryBpmModelPage,
  updateBpmModel,
} from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmModelList' });

interface BpmModelFormModel extends BpmModelAddForm {
  modelId?: number;
}

const router = useRouter();
const loading = ref(false);
const submitting = ref(false);
const publishingId = ref<number>();
const showSearchBar = ref(true);
const rows = ref<BpmModelRecord[]>([]);
const categoryOptions = ref<BpmCategoryRecord[]>([]);
const formOptions = ref<BpmFormRecord[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const formRef = ref<FormInstance>();

const searchForm = reactive({
  categoryId: undefined as number | undefined,
  formId: undefined as number | undefined,
  modelKey: '',
  modelName: '',
  visibleFlag: undefined as boolean | undefined,
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const formData = reactive<BpmModelFormModel>({
  categoryId: 0,
  description: '',
  formId: 0,
  formType: 1,
  instanceNoRuleId: null,
  modelKey: '',
  modelName: '',
  sort: 0,
  visibleFlag: true,
});

const rules: FormRules<BpmModelFormModel> = {
  categoryId: [{ required: true, message: '请选择流程分类', trigger: 'change' }],
  formId: [{ required: true, message: '请选择流程表单', trigger: 'change' }],
  formType: [{ required: true, message: '请选择表单类型', trigger: 'change' }],
  modelKey: [{ required: true, message: '请输入模型编码', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
};

const columnsFactory = (): ColumnOption<BpmModelRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'modelKey', label: '模型编码', minWidth: 160 },
  { prop: 'modelName', label: '模型名称', minWidth: 180 },
  { prop: 'categoryName', label: '分类', minWidth: 140, formatter: (row) => row.categoryName || '-' },
  { prop: 'formName', label: '表单', minWidth: 160, formatter: (row) => row.formName || '-' },
  { prop: 'visibleFlag', label: '可见状态', width: 100, align: 'center', useSlot: true },
  {
    prop: 'hasUnpublishedChanges',
    label: '草稿状态',
    width: 120,
    align: 'center',
    useSlot: true,
  },
  { prop: 'publishedDefinitionId', label: '已发布定义', width: 120, align: 'center' },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 220,
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
    categoryId: 0,
    description: '',
    formId: 0,
    formType: 1,
    instanceNoRuleId: null,
    modelId: undefined,
    modelKey: '',
    modelName: '',
    sort: 0,
    visibleFlag: true,
  });
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryBpmModelPage({
      categoryId: searchForm.categoryId,
      formId: searchForm.formId,
      modelKey: searchForm.modelKey,
      modelName: searchForm.modelName,
      pageNum: pagination.current,
      pageSize: pagination.size,
      visibleFlag: searchForm.visibleFlag,
    });
    rows.value = result?.list ?? [];
    pagination.total = result?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

async function loadReferenceOptions() {
  const [categoryPage, formPage] = await Promise.all([
    queryBpmCategoryPage({ pageNum: 1, pageSize: 200 }),
    queryBpmFormPage({ pageNum: 1, pageSize: 200 }),
  ]);
  categoryOptions.value = categoryPage?.list ?? [];
  formOptions.value = formPage?.list ?? [];
}

function handleSearch() {
  pagination.current = 1;
  void loadData();
}

function handleReset() {
  Object.assign(searchForm, {
    categoryId: undefined,
    formId: undefined,
    modelKey: '',
    modelName: '',
    visibleFlag: undefined,
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

async function openEditDialog(row: BpmModelRecord) {
  dialogMode.value = 'edit';
  const detail = await getBpmModelDetail(row.modelId);
  Object.assign(formData, {
    categoryId: detail.categoryId,
    description: detail.description || '',
    formId: detail.formId,
    formType: detail.formType,
    instanceNoRuleId: null,
    modelId: detail.modelId,
    modelKey: detail.modelKey,
    modelName: detail.modelName,
    sort: detail.sort ?? 0,
    visibleFlag: detail.visibleFlag ?? true,
  });
  dialogVisible.value = true;
}

function openDesigner(row: BpmModelRecord) {
  void router.push({
    path: '/system/bpm/model/designer',
    query: { modelId: String(row.modelId) },
  });
}

async function handlePublish(row: BpmModelRecord) {
  publishingId.value = row.modelId;
  try {
    await publishBpmDefinition({ modelId: row.modelId });
    ElMessage.success('流程定义发布成功');
    await loadData();
  } finally {
    publishingId.value = undefined;
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    if (dialogMode.value === 'add') {
      await addBpmModel(formData as BpmModelAddForm);
      ElMessage.success('流程模型新增成功');
    } else {
      await updateBpmModel(formData as Required<BpmModelFormModel>);
      ElMessage.success('流程模型更新成功');
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
  void Promise.all([loadReferenceOptions(), loadData()]).catch((error) => {
    ElMessage.error(error?.message || '流程模型数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="model-page">
      <ElCard v-show="showSearchBar" class="model-page__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem label="模型编码">
            <ElInput
              v-model="searchForm.modelKey"
              clearable
              placeholder="请输入模型编码"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="模型名称">
            <ElInput
              v-model="searchForm.modelName"
              clearable
              placeholder="请输入模型名称"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="分类">
            <ElSelect v-model="searchForm.categoryId" clearable filterable placeholder="请选择分类">
              <ElOption
                v-for="item in categoryOptions"
                :key="item.categoryId"
                :label="item.categoryName"
                :value="item.categoryId"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="表单">
            <ElSelect v-model="searchForm.formId" clearable filterable placeholder="请选择表单">
              <ElOption
                v-for="item in formOptions"
                :key="item.formId"
                :label="item.formName"
                :value="item.formId"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="可见状态">
            <ElSelect v-model="searchForm.visibleFlag" clearable placeholder="请选择状态">
              <ElOption label="可见" :value="true" />
              <ElOption label="隐藏" :value="false" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="model-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="openAddDialog">新增模型</ElButton>
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
            <template #visibleFlag="{ row }">
              <ElTag :type="row.visibleFlag ? 'success' : 'info'" effect="plain" size="small">
                {{ row.visibleFlag ? '可见' : '隐藏' }}
              </ElTag>
            </template>

            <template #hasUnpublishedChanges="{ row }">
              <ElTag
                :type="row.hasUnpublishedChanges ? 'warning' : 'success'"
                effect="plain"
                size="small"
              >
                {{ row.hasUnpublishedChanges ? '待发布' : '已同步' }}
              </ElTag>
            </template>

            <template #actions="{ row }">
              <ElSpace class="model-page__actions">
                <ElButton link size="small" type="primary" @click="openEditDialog(row)">
                  编辑
                </ElButton>
                <ElButton link size="small" type="primary" @click="openDesigner(row)">
                  设计
                </ElButton>
                <ElButton
                  :loading="publishingId === row.modelId"
                  link
                  size="small"
                  type="success"
                  @click="handlePublish(row)"
                >
                  发布
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <ElDialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增流程模型' : '编辑流程模型'"
      width="720px"
      @closed="resetForm"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
        <div class="model-page__form-grid">
          <ElFormItem label="模型编码" prop="modelKey">
            <ElInput v-model="formData.modelKey" placeholder="请输入模型编码" />
          </ElFormItem>
          <ElFormItem label="模型名称" prop="modelName">
            <ElInput v-model="formData.modelName" placeholder="请输入模型名称" />
          </ElFormItem>
          <ElFormItem label="流程分类" prop="categoryId">
            <ElSelect v-model="formData.categoryId" filterable placeholder="请选择流程分类">
              <ElOption
                v-for="item in categoryOptions"
                :key="item.categoryId"
                :label="item.categoryName"
                :value="item.categoryId"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="表单类型" prop="formType">
            <ElSelect v-model="formData.formType" placeholder="请选择表单类型">
              <ElOption label="内置表单" :value="1" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="流程表单" prop="formId">
            <ElSelect v-model="formData.formId" filterable placeholder="请选择流程表单">
              <ElOption
                v-for="item in formOptions"
                :key="item.formId"
                :label="item.formName"
                :value="item.formId"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="排序" prop="sort">
            <ElInputNumber v-model="formData.sort" :min="0" style="width: 100%" />
          </ElFormItem>
          <ElFormItem label="可见状态" prop="visibleFlag">
            <ElSwitch v-model="formData.visibleFlag" />
          </ElFormItem>
          <ElFormItem label="单号规则 ID" prop="instanceNoRuleId">
            <ElInputNumber
              v-model="formData.instanceNoRuleId"
              :min="0"
              :step="1"
              style="width: 100%"
            />
          </ElFormItem>
          <ElFormItem class="model-page__form-span-2" label="模型描述" prop="description">
            <ElInput
              v-model="formData.description"
              maxlength="500"
              placeholder="请输入模型描述"
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
.model-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.model-page__search-card,
.model-page__table-card {
  border-radius: 8px;
}

.model-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.model-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.model-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.model-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.model-page :deep(.art-table-panel),
.model-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.model-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.model-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.model-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.model-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.model-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.model-page__form-grid {
  display: grid;
  gap: 0 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.model-page__form-span-2 {
  grid-column: 1 / -1;
}

@media (width <= 768px) {
  .model-page__form-grid {
    grid-template-columns: 1fr;
  }

  .model-page__form-span-2 {
    grid-column: auto;
  }
}
</style>
