<script setup lang="ts">
import type {
  DepartmentAddForm,
  DepartmentRecord,
  DepartmentTreeRecord,
  DepartmentUpdateForm,
  EmployeeRecord,
} from '#/api/system/organization';
import type { FormInstance, FormRules } from 'element-plus';

import { computed, onMounted, reactive, ref } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  type ColumnOption,
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
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

import {
  addDepartment,
  deleteDepartment,
  listDepartmentTree,
  queryEmployeePage,
  updateDepartment,
} from '#/api/system/organization';

defineOptions({ name: 'SystemDepartmentList' });

interface DepartmentTreeRow extends DepartmentRecord {
  children?: DepartmentTreeRow[];
  level: number;
}

interface DepartmentFormModel extends DepartmentAddForm {
  departmentId?: number;
}

const loading = ref(false);
const keyword = ref('');
const showSearchBar = ref(true);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const addParentName = ref('');
const formRef = ref<FormInstance>();
const employees = ref<EmployeeRecord[]>([]);
const rawTree = ref<DepartmentTreeRecord[]>([]);

const formData = reactive<DepartmentFormModel>({
  departmentName: '',
  managerId: null,
  parentId: 0,
  sort: 100,
});

const rules: FormRules<DepartmentFormModel> = {
  departmentName: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
  ],
  sort: [{ required: true, message: '请输入排序', trigger: 'change' }],
};

const columnsFactory = (): ColumnOption<DepartmentTreeRow>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'departmentName', label: '部门名称', minWidth: 260, useSlot: true },
  {
    prop: 'managerName',
    label: '负责人',
    minWidth: 140,
    formatter: (row) => row.managerName || '-',
  },
  { prop: 'sort', label: '排序', width: 90, align: 'center' },
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

function buildDepartmentRows(
  nodes: DepartmentTreeRecord[],
  level = 0,
): DepartmentTreeRow[] {
  return nodes.map((node) => {
    const children = buildDepartmentRows(node.children ?? [], level + 1);

    return {
      children: children.length > 0 ? children : undefined,
      createTime: null,
      departmentId: node.departmentId,
      departmentName: node.departmentName,
      level,
      managerId: node.managerId ?? null,
      managerName: node.managerName ?? null,
      parentId: node.parentId ?? 0,
      sort: node.sort ?? 0,
      updateTime: null,
    };
  });
}

function flattenDepartmentRows(nodes: DepartmentTreeRow[]): DepartmentTreeRow[] {
  return nodes.flatMap((node) => {
    const { children, ...current } = node;
    return [current, ...flattenDepartmentRows(children ?? [])];
  });
}

const departmentTreeRows = computed(() => buildDepartmentRows(rawTree.value));
const departmentRows = computed(() =>
  flattenDepartmentRows(departmentTreeRows.value),
);

const filteredRows = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) {
    return departmentTreeRows.value;
  }

  return departmentRows.value.filter((item) =>
    [item.departmentName, item.managerName]
      .filter(Boolean)
      .some((field) => field!.toLowerCase().includes(value)),
  );
});

const parentOptions = computed(() => departmentRows.value);

const employeeOptions = computed(() => employees.value);

const isParentLocked = computed(() => dialogMode.value === 'add');

const dialogTitle = computed(() => {
  if (dialogMode.value === 'edit') {
    return '编辑部门';
  }

  return addParentName.value
    ? `新增${addParentName.value}下级部门`
    : '新增顶级部门';
});

function formatEmployeeOptionLabel(item: EmployeeRecord) {
  const departmentName = item.departmentName || '未分配部门';
  return `${item.actualName}（${departmentName}）`;
}

function resetForm() {
  Object.assign(formData, {
    departmentName: '',
    managerId: null,
    parentId: 0,
    sort: 100,
  });
  formData.departmentId = undefined;
  addParentName.value = '';
}

async function bootstrap() {
  loading.value = true;
  try {
    const [departmentTree, employeePage] = await Promise.all([
      listDepartmentTree(),
      queryEmployeePage({
        keyword: '',
        pageNum: 1,
        pageSize: 200,
      }),
    ]);
    rawTree.value = departmentTree ?? [];
    employees.value = employeePage?.list ?? [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  keyword.value = keyword.value.trim();
}

function handleReset() {
  keyword.value = '';
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function openAddRootDialog() {
  dialogMode.value = 'add';
  resetForm();
  dialogVisible.value = true;
}

function openAddChildDialog(row: DepartmentTreeRow) {
  dialogMode.value = 'add';
  resetForm();
  Object.assign(formData, {
    parentId: row.departmentId,
  });
  addParentName.value = row.departmentName;
  dialogVisible.value = true;
}

function openEditDialog(row: DepartmentTreeRow) {
  dialogMode.value = 'edit';
  addParentName.value = '';
  Object.assign(formData, {
    departmentId: row.departmentId,
    departmentName: row.departmentName,
    managerId: row.managerId ?? null,
    parentId: row.parentId ?? 0,
    sort: row.sort ?? 0,
  });
  dialogVisible.value = true;
}

async function handleDelete(row: DepartmentTreeRow) {
  try {
    await ElMessageBox.confirm(
      `确定删除部门“${row.departmentName}”吗？`,
      '删除确认',
      { type: 'warning' },
    );
    await deleteDepartment(row.departmentId);
    ElMessage.success('删除成功');
    await bootstrap();
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
    await addDepartment(formData as DepartmentAddForm);
    ElMessage.success('新增部门成功');
  } else {
    await updateDepartment(formData as DepartmentUpdateForm);
    ElMessage.success('更新部门成功');
  }

  dialogVisible.value = false;
  await bootstrap();
}

onMounted(() => {
  void bootstrap().catch((error) => {
    ElMessage.error(error?.message || '部门数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="department-page">
      <ElCard
        v-show="showSearchBar"
        class="department-page__search-card"
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
          <ElFormItem class="department-page__keyword-item" label="关键字">
            <ElInput
              v-model="keyword"
              clearable
              placeholder="请输入部门名称或负责人"
              @keyup.enter="handleSearch"
            />
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="department-page__table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="handleToggleSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="openAddRootDialog">
                新增顶级部门
              </ElButton>
            </template>
          </ArtTableHeader>

            <ArtTable
              :columns="columns"
              :data="filteredRows"
              :default-expand-all="true"
              height="100%"
              :indent="24"
              :loading="loading"
              row-key="departmentId"
              :tree-props="{ children: 'children' }"
            >
            <template #departmentName="{ row }">
              <div
                class="department-page__tree-name"
                :style="{
                  paddingLeft: keyword.trim() ? `${row.level * 24}px` : '0px',
                }"
              >
                <span
                  v-if="!row.children?.length"
                  class="department-page__tree-line"
                />
                <span class="department-page__tree-label">{{ row.departmentName }}</span>
                <ElTag
                  v-if="row.level === 0"
                  effect="plain"
                  size="small"
                  type="primary"
                >
                  根节点
                </ElTag>
              </div>
            </template>

            <template #actions="{ row }">
              <ElSpace class="department-page__actions">
                <ElButton
                  link
                  size="small"
                  type="primary"
                  @click="openAddChildDialog(row)"
                >
                  新增下级
                </ElButton>
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
      :title="dialogTitle"
      width="520px"
      @closed="resetForm"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
        <ElFormItem label="部门名称" prop="departmentName">
          <ElInput v-model="formData.departmentName" placeholder="请输入部门名称" />
        </ElFormItem>
        <ElFormItem label="上级部门" prop="parentId">
          <ElSelect
            v-model="formData.parentId"
            clearable
            :disabled="isParentLocked"
            filterable
            placeholder="请选择上级部门"
          >
            <ElOption :value="0" label="顶级部门" />
            <ElOption
              v-for="item in parentOptions"
              :key="item.departmentId"
              :disabled="dialogMode === 'edit' && item.departmentId === formData.departmentId"
              :label="item.departmentName"
              :value="item.departmentId"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="部门负责人" prop="managerId">
          <ElSelect v-model="formData.managerId" clearable filterable placeholder="请选择负责人">
            <ElOption
              v-for="item in employeeOptions"
              :key="item.employeeId"
              :label="formatEmployeeOptionLabel(item)"
              :value="item.employeeId"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="排序" prop="sort">
          <ElInputNumber v-model="formData.sort" :min="0" style="width: 100%" />
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
.department-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.department-page__search-card,
.department-page__table-card {
  border-radius: 8px;
}

.department-page__search-card {
  background: var(--el-bg-color);
  border: 0;
  flex-shrink: 0;
}

.department-page__search-card :deep(.el-card__body) {
  padding: 16px;
}

.department-page__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.department-page__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.department-page :deep(.art-table-panel),
.department-page :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.department-page :deep(.art-table-header) {
  margin-bottom: 18px;
}

.department-page :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.department-page__keyword-item :deep(.el-form-item__content) {
  width: 260px;
}

.department-page__tree-name {
  align-items: center;
  display: inline-flex;
  gap: 8px;
}

.department-page__tree-line {
  background: var(--el-border-color);
  border-radius: 999px;
  display: inline-block;
  height: 8px;
  width: 8px;
}

.department-page__tree-label {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.department-page__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.department-page__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.department-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (width <= 768px) {
  .department-page__keyword-item :deep(.el-form-item__content) {
    width: 100%;
  }
}
</style>
