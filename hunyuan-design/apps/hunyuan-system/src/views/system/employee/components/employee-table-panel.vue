<script setup lang="ts">
import type {
  DepartmentRecord,
  EmployeeRecord,
  PositionRecord,
  RoleRecord,
} from '#/api/system/organization';

import { computed, onMounted, ref, watch } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  type ColumnOption,
  type TableError,
  useTable,
} from '@vben/art-hooks/table';

import {
  ElAvatar,
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
  batchDeleteEmployees,
  batchUpdateDepartment,
  queryEmployeePage,
  toggleEmployeeStatus,
} from '#/api/system/organization';

defineOptions({ name: 'EmployeeTablePanel' });

const props = defineProps<EmployeeTablePanelProps>();

const emit = defineEmits<{
  add: [];
  'department-change': [departmentId: null | number];
  edit: [row: EmployeeRecord];
  'total-change': [total: number];
}>();

interface EmployeeTablePanelProps {
  departments: DepartmentRecord[];
  positions: PositionRecord[];
  roles: RoleRecord[];
  selectedDepartmentId: null | number;
}

const showSearchBar = ref(true);
const selectedRows = ref<EmployeeRecord[]>([]);
const searchParamsProxy = computed(() => searchParams as any);
const transferDepartmentId = ref<null | number>(null);
const transferDialogVisible = ref(false);
const transferLoading = ref(false);

const departmentOptions = computed(() =>
  [...props.departments].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0)),
);

const positionNameMap = computed(() =>
  Object.fromEntries(
    props.positions.map((item) => [item.positionId, item.positionName]),
  ) as Record<number, string>,
);

const roleNameMap = computed(
  () =>
    Object.fromEntries(
      props.roles.map((item) => [item.roleId, item.roleName]),
    ) as Record<number, string>,
);

const columnsFactory = (): ColumnOption<EmployeeRecord>[] => [
  { type: 'selection', width: 50, align: 'center' },
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'actualName', label: '姓名', minWidth: 130, useSlot: true },
  { prop: 'loginName', label: '登录账号', minWidth: 130, useSlot: true },
  {
    prop: 'departmentName',
    label: '所属部门',
    minWidth: 190,
    showOverflowTooltip: true,
  },
  { prop: 'positionName', label: '岗位', minWidth: 125, useSlot: true },
  { prop: 'roleNameList', label: '角色', minWidth: 120, useSlot: true },
  {
    prop: 'actions',
    label: '操作',
    width: 180,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const {
  data,
  loading,
  pagination,
  searchParams,
  columns,
  columnChecks,
  handleCurrentChange,
  handleSizeChange,
  getData,
  replaceSearchParams,
  resetSearchParams,
} = useTable({
  core: {
    apiFn: queryEmployeePage,
    apiParams: {
      keyword: '',
      departmentId: props.selectedDepartmentId,
    },
    columnsFactory,
    immediate: false,
    paginationKey: {
      current: 'pageNum',
      size: 'pageSize',
    },
  },
  hooks: {
    onError: (error: TableError) => {
      ElMessage.error(error.message);
    },
  },
});

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

// 左侧机构树是部门筛选的主入口；右侧下拉变化时也要同步回父页面，保持选中态一致。
watch(
  () => props.selectedDepartmentId,
  async (departmentId) => {
    replaceSearchParams({
      departmentId,
      pageNum: 1,
    } as any);
    searchParamsProxy.value.departmentId = departmentId;
    selectedRows.value = [];
    await getData();
  },
);

watch(
  () => pagination.total,
  (total) => {
    emit('total-change', total);
  },
  { immediate: true },
);

function formatPositionName(row: EmployeeRecord) {
  if (row.positionName) {
    return row.positionName;
  }
  if (row.positionId) {
    return positionNameMap.value[row.positionId] || `#${row.positionId}`;
  }
  return '-';
}

function formatRoleNames(row: EmployeeRecord) {
  if (row.roleNameList?.length) {
    return row.roleNameList;
  }
  if (row.roleIdList?.length) {
    return row.roleIdList.map((id) => roleNameMap.value[id] || `#${id}`);
  }
  return [];
}

function getAvatarText(row: EmployeeRecord) {
  return (
    row.actualName?.trim()?.slice(0, 1) || row.loginName?.slice(0, 1) || '-'
  );
}

function getPrimaryRole(row: EmployeeRecord) {
  return (
    formatRoleNames(row)[0] || (row.administratorFlag ? '管理员' : '普通员工')
  );
}

function getRoleTagType(roleName: string) {
  if (roleName.includes('管理员')) {
    return 'primary';
  }
  if (roleName.includes('产品')) {
    return 'success';
  }
  if (roleName.includes('设计')) {
    return 'info';
  }
  if (roleName.includes('销售') || roleName.includes('运营')) {
    return 'warning';
  }
  return 'primary';
}

function getRoleTagClass(roleName: string) {
  if (roleName.includes('设计')) {
    return 'employee-table-panel__role-tag--design';
  }
  if (roleName.includes('开发') || roleName.includes('测试')) {
    return 'employee-table-panel__role-tag--cyan';
  }
  return '';
}

async function reload() {
  await getData();
}

function handleSearch() {
  replaceSearchParams({
    departmentId: searchParamsProxy.value.departmentId ?? undefined,
    disabledFlag: searchParamsProxy.value.disabledFlag,
    keyword: searchParamsProxy.value.keyword?.trim() || undefined,
  });
  void getData();
}

async function handleReset() {
  await resetSearchParams();
  replaceSearchParams({
    departmentId: props.selectedDepartmentId,
  } as any);
  searchParamsProxy.value.departmentId = props.selectedDepartmentId;
  selectedRows.value = [];
  ElMessage.success('已重置筛选条件');
  await getData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

function handleDepartmentChange(value: number | string | undefined) {
  emit(
    'department-change',
    value === undefined || value === '' ? null : Number(value),
  );
}

async function handleToggleStatus(row: EmployeeRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要${row.disabledFlag ? '启用' : '停用'}员工"${row.actualName}"吗？`,
      '提示',
      { type: 'warning' },
    );
    await toggleEmployeeStatus(row.employeeId);
    ElMessage.success('操作成功');
    await getData();
  } catch {
    // 用户取消操作
  }
}

async function handleDelete(row: EmployeeRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要删除员工"${row.actualName}"吗？此操作不可恢复。`,
      '删除确认',
      { type: 'warning' },
    );
    await batchDeleteEmployees([row.employeeId]);
    ElMessage.success('删除成功');
    await getData();
  } catch {
    // 用户取消操作
  }
}

async function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要删除的员工');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 名员工吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning' },
    );
    const ids = selectedRows.value.map((row) => row.employeeId);
    await batchDeleteEmployees(ids);
    ElMessage.success('批量删除成功');
    selectedRows.value = [];
    await getData();
  } catch {
    // 用户取消操作
  }
}

function handleOpenBatchTransfer() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要转移部门的员工');
    return;
  }
  transferDepartmentId.value = props.selectedDepartmentId;
  transferDialogVisible.value = true;
}

async function handleBatchTransferDepartment() {
  if (!transferDepartmentId.value) {
    ElMessage.warning('请选择目标部门');
    return;
  }

  transferLoading.value = true;
  try {
    await batchUpdateDepartment({
      departmentId: transferDepartmentId.value,
      employeeIdList: selectedRows.value.map((row) => row.employeeId),
    });
    ElMessage.success('批量转移部门成功');
    selectedRows.value = [];
    transferDialogVisible.value = false;
    await getData();
  } finally {
    transferLoading.value = false;
  }
}

function handleSelectionChange(rows: EmployeeRecord[]) {
  selectedRows.value = rows;
}

onMounted(() => {
  void getData();
});

defineExpose({
  reload,
});
</script>

<template>
  <div class="employee-table-panel">
    <ElCard
      v-show="showSearchBar"
      class="employee-table-panel__search-card"
      shadow="never"
    >
      <ArtSearchPanel
        :loading="loading"
        reset-text="重置"
        search-text="查询"
        :show-refresh="false"
        @reset="handleReset"
        @search="handleSearch"
      >
        <ElFormItem class="employee-table-panel__keyword-item" label="关键字">
          <ElInput
            v-model="searchParamsProxy.keyword"
            clearable
            placeholder="请输入姓名、账号或手机号"
            @keyup.enter="handleSearch"
          />
        </ElFormItem>
        <ElFormItem class="employee-table-panel__department-item" label="所属部门">
          <ElSelect
            v-model="searchParamsProxy.departmentId"
            clearable
            filterable
            placeholder="请选择部门"
            @change="handleDepartmentChange"
          >
            <ElOption
              v-for="department in departmentOptions"
              :key="department.departmentId"
              :label="department.departmentName"
              :value="department.departmentId"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem class="employee-table-panel__status-item" label="状态">
          <ElSelect
            v-model="searchParamsProxy.disabledFlag"
            class="employee-table-panel__status-select"
            clearable
            placeholder="请选择状态"
          >
            <ElOption :value="false" label="启用" />
            <ElOption :value="true" label="停用" />
          </ElSelect>
        </ElFormItem>
      </ArtSearchPanel>
    </ElCard>

    <ElCard class="employee-table-panel__table-card" shadow="never">
      <ArtTablePanel>
        <ArtTableHeader
          v-model="columnChecks"
          :loading="loading"
          :show-search-bar="showSearchBar"
          layout="search,size,fullscreen,columns,settings"
          @search="handleToggleSearchBar"
        >
          <template #left>
            <ElSpace>
              <ElButton type="primary" @click="emit('add')">新增员工</ElButton>
              <ElButton
                :disabled="selectedRows.length === 0"
                @click="handleBatchDelete"
              >
                批量删除
              </ElButton>
              <ElButton
                :disabled="selectedRows.length === 0"
                @click="handleOpenBatchTransfer"
              >
                批量转移部门
              </ElButton>
            </ElSpace>
          </template>
        </ArtTableHeader>

        <ArtTable
          :columns="columns"
          :data="data"
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
          <template #actualName="{ row }">
            <div class="employee-table-panel__employee">
              <ElAvatar
                class="employee-table-panel__avatar"
                :size="28"
                :src="row.avatar || undefined"
              >
                {{ getAvatarText(row) }}
              </ElAvatar>
              <span class="employee-table-panel__primary">
                {{ row.actualName || '-' }}
              </span>
            </div>
          </template>

          <template #loginName="{ row }">
            <span class="employee-table-panel__secondary">
              {{ row.loginName || '-' }}
            </span>
          </template>

          <template #positionName="{ row }">
            <span>{{ formatPositionName(row) }}</span>
          </template>

          <template #roleNameList="{ row }">
            <ElTag
              :class="getRoleTagClass(getPrimaryRole(row))"
              :type="getRoleTagType(getPrimaryRole(row))"
              effect="light"
              size="small"
            >
              {{ getPrimaryRole(row) }}
            </ElTag>
          </template>

          <template #actions="{ row }">
            <ElSpace class="employee-table-panel__actions">
              <ElButton
                link
                size="small"
                type="primary"
                @click="emit('edit', row)"
              >
                编辑
              </ElButton>
              <ElButton
                link
                size="small"
                :type="row.disabledFlag ? 'success' : 'warning'"
                @click="handleToggleStatus(row)"
              >
                {{ row.disabledFlag ? '启用' : '停用' }}
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

    <ElDialog
      v-model="transferDialogVisible"
      title="批量转移部门"
      width="420px"
    >
      <ElFormItem label="目标部门">
        <ElSelect
          v-model="transferDepartmentId"
          filterable
          placeholder="请选择目标部门"
          style="width: 100%"
        >
          <ElOption
            v-for="department in departmentOptions"
            :key="department.departmentId"
            :label="department.departmentName"
            :value="department.departmentId"
          />
        </ElSelect>
      </ElFormItem>
      <div class="employee-table-panel__transfer-tip">
        将选中的 {{ selectedRows.length }} 名员工转移到目标部门。
      </div>
      <template #footer>
        <ElSpace>
          <ElButton @click="transferDialogVisible = false">取消</ElButton>
          <ElButton
            :loading="transferLoading"
            type="primary"
            @click="handleBatchTransferDepartment"
          >
            确定转移
          </ElButton>
        </ElSpace>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.employee-table-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.employee-table-panel__search-card,
.employee-table-panel__table-card {
  border-radius: 8px;
}

.employee-table-panel__search-card {
  background: var(--el-bg-color);
  border: 0;
  border-radius: 8px;
  flex-shrink: 0;
}

.employee-table-panel__search-card :deep(.el-card__body) {
  padding: 16px;
}

.employee-table-panel__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.employee-table-panel__table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 16px;
}

.employee-table-panel :deep(.art-search-panel .el-form-item) {
  margin-bottom: 0;
}

.employee-table-panel :deep(.art-search-panel__actions) {
  margin-bottom: 0;
}

.employee-table-panel :deep(.art-search-panel .el-input),
.employee-table-panel :deep(.art-search-panel .el-select) {
  width: 100%;
}

.employee-table-panel :deep(.employee-table-panel__keyword-item .el-form-item__content) {
  flex: 0 0 216px;
  width: 216px;
}

.employee-table-panel :deep(.employee-table-panel__department-item .el-form-item__content),
.employee-table-panel :deep(.employee-table-panel__status-item .el-form-item__content) {
  flex: 0 0 168px;
  width: 168px;
}

.employee-table-panel :deep(.art-table-panel),
.employee-table-panel :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.employee-table-panel :deep(.art-table-header) {
  margin-bottom: 18px;
}

.employee-table-panel :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.employee-table-panel__status-select {
  width: 100%;
}

.employee-table-panel__primary {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.employee-table-panel__secondary {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.employee-table-panel__employee {
  align-items: center;
  display: inline-flex;
  gap: 10px;
}

.employee-table-panel__avatar {
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
}

.employee-table-panel__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
}

.employee-table-panel__transfer-tip {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 22px;
  margin-top: 8px;
}

.employee-table-panel :deep(.el-tag) {
  border: 0;
  border-radius: 4px;
  font-weight: 500;
}

.employee-table-panel :deep(.employee-table-panel__role-tag--design) {
  color: #7c3aed;
  background-color: #f3e8ff;
}

.employee-table-panel :deep(.employee-table-panel__role-tag--cyan) {
  color: #0284c7;
  background-color: #e0f2fe;
}

@media (width <= 768px) {
  .employee-table-panel :deep(.el-card__body) {
    padding: 8px;
  }

  .employee-table-panel__keyword-item,
  .employee-table-panel__department-item,
  .employee-table-panel__status-item {
    width: 100%;
  }

  .employee-table-panel :deep(.employee-table-panel__keyword-item .el-form-item__content),
  .employee-table-panel :deep(.employee-table-panel__department-item .el-form-item__content),
  .employee-table-panel :deep(.employee-table-panel__status-item .el-form-item__content) {
    flex: 1 1 auto;
    width: 100%;
  }
}
</style>
