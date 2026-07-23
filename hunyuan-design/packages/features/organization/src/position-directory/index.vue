<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus';

import type { ColumnOption } from '@vben/art-hooks/table';

import type { PositionCommand, PositionRecord } from './contract';

import { computed, inject, onMounted, reactive, ref } from 'vue';

import { AccessControl } from '@vben/access';
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

import { organizationPositionClientKey } from './dependencies';

defineOptions({ name: 'OrganizationPositionDirectory' });

const loading = ref(false);
const keyword = ref('');
const activeKeyword = ref('');
const showSearchBar = ref(true);
const allRows = ref<PositionRecord[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const formRef = ref<FormInstance>();
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

interface PositionFormModel extends PositionCommand {
  positionId?: number;
}

const injectedClient = inject(organizationPositionClientKey);
if (!injectedClient) {
  throw new Error('组织岗位客户端尚未注册');
}
const client = injectedClient;

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

const filteredRows = computed(() => {
  const value = activeKeyword.value.trim().toLowerCase();
  if (!value) {
    return allRows.value;
  }
  return allRows.value.filter((item) =>
    [item.positionName, item.positionLevel]
      .filter(Boolean)
      .some((field) => field!.toLowerCase().includes(value)),
  );
});
const rows = computed(() => {
  const start = (pagination.current - 1) * pagination.size;
  return filteredRows.value.slice(start, start + pagination.size);
});
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
    allRows.value = (await client.list()) ?? [];
    pagination.total = filteredRows.value.length;
    const lastPage = Math.max(1, Math.ceil(pagination.total / pagination.size));
    pagination.current = Math.min(pagination.current, lastPage);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  activeKeyword.value = keyword.value;
  pagination.current = 1;
  pagination.total = filteredRows.value.length;
}

function handleReset() {
  keyword.value = '';
  activeKeyword.value = '';
  pagination.current = 1;
  pagination.total = allRows.value.length;
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
    await client.delete(row.positionId);
    ElMessage.success('岗位已删除');
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

  const command: PositionCommand = {
    positionLevel: formData.positionLevel,
    positionName: formData.positionName,
    remark: formData.remark,
    sort: formData.sort,
  };
  if (dialogMode.value === 'add') {
    await client.create(command);
    ElMessage.success('岗位已创建');
  } else {
    await client.update(formData.positionId!, command);
    ElMessage.success('岗位已更新');
  }

  dialogVisible.value = false;
  await loadData();
}

function handleCurrentChange(value: number) {
  pagination.current = value;
}

function handleSizeChange(value: number) {
  pagination.size = value;
  pagination.current = 1;
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
              <AccessControl
                :codes="['organization.position.create']"
                type="code"
              >
                <ElButton type="primary" @click="openAddDialog">新增职务</ElButton>
              </AccessControl>
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
              <ElSpace class="position-page__actions">
                <AccessControl
                  :codes="['organization.position.update']"
                  type="code"
                >
                  <ElButton
                    link
                    size="small"
                    type="primary"
                    @click="openEditDialog(row)"
                  >
                    编辑
                  </ElButton>
                </AccessControl>
                <AccessControl
                  :codes="['organization.position.delete']"
                  type="code"
                >
                  <ElButton
                    link
                    size="small"
                    type="danger"
                    @click="handleDelete(row)"
                  >
                    删除
                  </ElButton>
                </AccessControl>
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
