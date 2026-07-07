<script setup lang="ts">
import type { BpmCategoryAddForm, BpmCategoryRecord } from '#/api/system/bpm';
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
  ElInputNumber,
  ElMessage,
  ElOption,
  ElSelect,
  ElSpace,
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  addBpmCategory,
  getBpmCategoryDetail,
  queryBpmCategoryPage,
  updateBpmCategory,
} from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmCategoryList' });

interface BpmCategoryFormModel extends BpmCategoryAddForm {
  categoryId?: number;
}

const loading = ref(false);
const submitting = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmCategoryRecord[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const formRef = ref<FormInstance>();

const searchForm = reactive({
  categoryCode: '',
  categoryName: '',
  disabledFlag: undefined as boolean | undefined,
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const formData = reactive<BpmCategoryFormModel>({
  categoryCode: '',
  categoryName: '',
  disabledFlag: false,
  icon: '',
  remark: '',
  sort: 0,
});

const rules: FormRules<BpmCategoryFormModel> = {
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
};

const columnsFactory = (): ColumnOption<BpmCategoryRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'categoryCode', label: '分类编码', minWidth: 160 },
  { prop: 'categoryName', label: '分类名称', minWidth: 180 },
  {
    prop: 'icon',
    label: '图标',
    minWidth: 140,
    formatter: (row) => row.icon || '-',
  },
  { prop: 'sort', label: '排序', width: 90, align: 'center' },
  { prop: 'disabledFlag', label: '状态', width: 100, align: 'center', useSlot: true },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
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

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function resetForm() {
  Object.assign(formData, {
    categoryCode: '',
    categoryId: undefined,
    categoryName: '',
    disabledFlag: false,
    icon: '',
    remark: '',
    sort: 0,
  });
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryBpmCategoryPage({
      categoryCode: searchForm.categoryCode,
      categoryName: searchForm.categoryName,
      disabledFlag: searchForm.disabledFlag,
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
    categoryCode: '',
    categoryName: '',
    disabledFlag: undefined,
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

async function openEditDialog(row: BpmCategoryRecord) {
  dialogMode.value = 'edit';
  const detail = await getBpmCategoryDetail(row.categoryId);
  Object.assign(formData, {
    categoryCode: detail.categoryCode,
    categoryId: detail.categoryId,
    categoryName: detail.categoryName,
    disabledFlag: detail.disabledFlag ?? false,
    icon: detail.icon || '',
    remark: detail.remark || '',
    sort: detail.sort ?? 0,
  });
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    if (dialogMode.value === 'add') {
      await addBpmCategory(formData as BpmCategoryAddForm);
      ElMessage.success('流程分类新增成功');
    } else {
      await updateBpmCategory(formData as Required<BpmCategoryFormModel>);
      ElMessage.success('流程分类更新成功');
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
    ElMessage.error(error?.message || '流程分类数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="category-page">
      <ElCard v-show="showSearchBar" class="category-page__search-card" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          reset-text="重置"
          search-text="查询"
          :show-refresh="false"
          @reset="handleReset"
          @search="handleSearch"
        >
          <ElFormItem label="分类编码">
            <ElInput
              v-model="searchForm.categoryCode"
              clearable
              placeholder="请输入分类编码"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
          <ElFormItem label="分类名称">
            <ElInput
              v-model="searchForm.categoryName"
              clearable
              placeholder="请输入分类名称"
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

      <ElCard class="category-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="openAddDialog">新增分类</ElButton>
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
              <ElSpace class="category-page__actions">
                <ElButton link size="small" type="primary" @click="openEditDialog(row)">
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
      :title="dialogMode === 'add' ? '新增流程分类' : '编辑流程分类'"
      width="640px"
      @closed="resetForm"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
        <div class="category-page__form-grid">
          <ElFormItem label="分类编码" prop="categoryCode">
            <ElInput v-model="formData.categoryCode" placeholder="请输入分类编码" />
          </ElFormItem>
          <ElFormItem label="分类名称" prop="categoryName">
            <ElInput v-model="formData.categoryName" placeholder="请输入分类名称" />
          </ElFormItem>
          <ElFormItem label="图标" prop="icon">
            <ElInput v-model="formData.icon" placeholder="例如 ep:connection" />
          </ElFormItem>
          <ElFormItem label="排序" prop="sort">
            <ElInputNumber v-model="formData.sort" :min="0" style="width: 100%" />
          </ElFormItem>
          <ElFormItem label="禁用状态" prop="disabledFlag">
            <ElSwitch v-model="formData.disabledFlag" />
          </ElFormItem>
          <ElFormItem class="category-page__form-span-2" label="备注" prop="remark">
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
.category-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.category-page__search-card,
.category-page__table-card {
  border-radius: 8px;
}

.category-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.category-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.category-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.category-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.category-page :deep(.art-table-panel),
.category-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.category-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.category-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.category-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.category-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.category-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.category-page__form-grid {
  display: grid;
  gap: 0 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.category-page__form-span-2 {
  grid-column: 1 / -1;
}

@media (width <= 768px) {
  .category-page__form-grid {
    grid-template-columns: 1fr;
  }

  .category-page__form-span-2 {
    grid-column: auto;
  }
}
</style>
