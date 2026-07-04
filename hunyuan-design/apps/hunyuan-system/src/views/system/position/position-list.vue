<script setup lang="ts">
import type {
  PositionAddForm,
  PositionRecord,
  PositionUpdateForm,
} from '#/api/system/organization';
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
  ElMessageBox,
  ElSpace,
} from 'element-plus';

import {
  addPosition,
  batchDeletePositions,
  deletePosition,
  queryPositionPage,
  updatePosition,
} from '#/api/system/organization';

defineOptions({ name: 'SystemPositionList' });

const loading = ref(false);
const keyword = ref('');
const showSearchBar = ref(true);
const rows = ref<PositionRecord[]>([]);
const selectedRows = ref<PositionRecord[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const formRef = ref<FormInstance>();
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

interface PositionFormModel extends PositionAddForm {
  positionId?: number;
}

const formData = reactive<PositionFormModel>({
  positionLevel: '',
  positionName: '',
  remark: '',
  sort: 100,
});

const rules: FormRules<PositionFormModel> = {
  positionName: [
    { required: true, message: '请输入职务名称', trigger: 'blur' },
  ],
  sort: [{ required: true, message: '请输入排序', trigger: 'change' }],
};

const columnsFactory = (): ColumnOption<PositionRecord>[] => [
  { type: 'selection', width: 50, align: 'center' },
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'positionName', label: '职务名称', minWidth: 180 },
  {
    prop: 'positionLevel',
    label: '职级',
    minWidth: 120,
    formatter: (row) => row.positionLevel || '-',
  },
  { prop: 'sort', label: '排序', width: 90, align: 'center' },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 220,
    formatter: (row) => row.remark || '-',
  },
  { prop: 'updateTime', label: '更新时间', minWidth: 180 },
  {
    prop: 'actions',
    label: '操作',
    width: 136,
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
    positionLevel: '',
    positionName: '',
    remark: '',
    sort: 100,
  });
  formData.positionId = undefined;
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryPositionPage({
      keywords: keyword.value,
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

function openEditDialog(row: PositionRecord) {
  dialogMode.value = 'edit';
  Object.assign(formData, {
    positionId: row.positionId,
    positionLevel: row.positionLevel || '',
    positionName: row.positionName,
    remark: row.remark || '',
    sort: row.sort ?? 0,
  });
  dialogVisible.value = true;
}

async function handleDelete(row: PositionRecord) {
  try {
    await ElMessageBox.confirm(
      `确定删除职务“${row.positionName}”吗？`,
      '删除确认',
      { type: 'warning' },
    );
    await deletePosition(row.positionId);
    ElMessage.success('删除成功');
    await loadData();
  } catch {
    // 用户取消
  }
}

async function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要删除的职务');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedRows.value.length} 个职务吗？`,
      '批量删除确认',
      { type: 'warning' },
    );
    await batchDeletePositions(selectedRows.value.map((item) => item.positionId));
    ElMessage.success('批量删除成功');
    selectedRows.value = [];
    await loadData();
  } catch {
    // 用户取消
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  if (dialogMode.value === 'add') {
    await addPosition(formData as PositionAddForm);
    ElMessage.success('新增职务成功');
  } else {
    await updatePosition(formData as PositionUpdateForm);
    ElMessage.success('更新职务成功');
  }

  dialogVisible.value = false;
  await loadData();
}

function handleSelectionChange(value: PositionRecord[]) {
  selectedRows.value = value;
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
    ElMessage.error(error?.message || '职务数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="position-page">
      <ElCard
        v-show="showSearchBar"
        class="position-page__search-card"
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
          <ElFormItem class="position-page__keyword-item" label="关键字">
            <ElInput
              v-model="keyword"
              clearable
              placeholder="请输入职务名称或职级"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="position-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElSpace>
                <ElButton type="primary" @click="openAddDialog">新增职务</ElButton>
                <ElButton
                  :disabled="selectedRows.length === 0"
                  @click="handleBatchDelete"
                >
                  批量删除
                </ElButton>
              </ElSpace>
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
            @selection-change="handleSelectionChange"
          >
            <template #actions="{ row }">
              <ElSpace class="position-page__actions">
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openEditDialog(row)"
                >
                  编辑
                </ElButton>
                <ElButton
                  link
                  size="small"
                  type="danger"
                  @click="handleDelete(row)"
                >
                  删除
                </ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <ElDialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增职务' : '编辑职务'"
      width="520px"
      @closed="resetForm"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
        <ElFormItem label="职务名称" prop="positionName">
          <ElInput v-model="formData.positionName" placeholder="请输入职务名称" />
        </ElFormItem>
        <ElFormItem label="职级" prop="positionLevel">
          <ElInput v-model="formData.positionLevel" placeholder="请输入职级" />
        </ElFormItem>
        <ElFormItem label="排序" prop="sort">
          <ElInputNumber v-model="formData.sort" :min="0" style="width: 100%" />
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
.position-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.position-page__search-card,
.position-page__table-card {
  border-radius: 8px;
}

.position-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.position-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.position-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.position-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.position-page :deep(.art-table-panel),
.position-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.position-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.position-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.position-page__keyword-item :deep(.el-form-item__content) {
  width: 260px;
}

.position-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.position-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.position-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .position-page__keyword-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
