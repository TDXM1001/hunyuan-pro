<script setup lang="ts">
import type {
  DepartmentOption,
  EmployeeRecord,
  PositionOption,
} from '../contract';

import { computed, inject, onMounted, ref, watch } from 'vue';

import { AccessControl } from '@vben/access';
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

import { employeeClientKey } from '../dependencies';

defineOptions({ name: 'EmployeeTablePanel' });

interface EmployeeTablePanelProps {
  departments: DepartmentOption[];
  positions: PositionOption[];
  selectedDepartmentId: null | number;
  showDepartmentFilter?: boolean;
}

const props = withDefaults(defineProps<EmployeeTablePanelProps>(), {
  showDepartmentFilter: false,
});
const emit = defineEmits<{
  add: [];
  'department-change': [departmentId: null | number];
  edit: [row: EmployeeRecord];
  'total-change': [total: number];
}>();

function requireDependency<T>(dependency: T | undefined, name: string): T {
  if (!dependency) {
    throw new Error(`${name} is not registered`);
  }
  return dependency;
}

const client = requireDependency(
  inject(employeeClientKey),
  'identity employee client',
);

const showSearchBar = ref(true);
const selectedRows = ref<EmployeeRecord[]>([]);
const searchParamsProxy = computed(() => searchParams as any);
const transferDepartmentId = ref<null | number>(null);
const transferDialogVisible = ref(false);
const transferLoading = ref(false);
const credentialDialogVisible = ref(false);
const temporaryPassword = ref('');

const departmentOptions = computed(() =>
  [...props.departments].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0)),
);
const positionNameMap = computed(
  () =>
    Object.fromEntries(
      props.positions.map((item) => [item.positionId, item.positionName]),
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
  { prop: 'disabledFlag', label: '状态', minWidth: 90, useSlot: true },
  {
    prop: 'actions',
    label: '操作',
    width: 310,
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
    apiFn: client.query,
    apiParams: {
      keyword: '',
      departmentId: props.selectedDepartmentId,
    },
    columnsFactory,
    immediate: false,
    paginationKey: { current: 'pageNum', size: 'pageSize' },
  },
  hooks: {
    onError: (error: TableError) => ElMessage.error(error.message),
  },
});

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

watch(
  () => props.selectedDepartmentId,
  async (departmentId) => {
    replaceSearchParams({ departmentId, pageNum: 1 } as any);
    searchParamsProxy.value.departmentId = departmentId;
    selectedRows.value = [];
    await getData();
  },
);

watch(
  () => pagination.total,
  (total) => emit('total-change', total),
  { immediate: true },
);

function formatPositionName(row: EmployeeRecord) {
  if (row.positionName) return row.positionName;
  if (row.positionId) return positionNameMap.value[row.positionId] || `#${row.positionId}`;
  return '-';
}

function getAvatarText(row: EmployeeRecord) {
  return row.actualName?.trim()?.slice(0, 1) || row.loginName?.slice(0, 1) || '-';
}

async function reload() {
  await getData();
}

function handleSearch() {
  replaceSearchParams({
    departmentId: searchParamsProxy.value.departmentId ?? undefined,
    disabled: searchParamsProxy.value.disabled,
    keyword: searchParamsProxy.value.keyword?.trim() || undefined,
  } as any);
  void getData();
}

async function handleReset() {
  await resetSearchParams();
  replaceSearchParams({ departmentId: props.selectedDepartmentId } as any);
  searchParamsProxy.value.departmentId = props.selectedDepartmentId;
  selectedRows.value = [];
  await getData();
}

function handleDepartmentChange(value: number | string | undefined) {
  emit('department-change', value === undefined || value === '' ? null : Number(value));
}

async function handleToggleStatus(row: EmployeeRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要${row.disabledFlag ? '启用' : '停用'}员工"${row.actualName}"吗？`,
      '提示',
      { type: 'warning' },
    );
    if (row.disabledFlag) {
      await client.enable(row.employeeId);
    } else {
      await client.disable(row.employeeId);
    }
    ElMessage.success('操作成功');
    await getData();
  } catch {
    // The confirmation dialog is cancellable.
  }
}

async function handleResetPassword(row: EmployeeRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要重置员工"${row.actualName}"的密码吗？`,
      '重置密码',
      { type: 'warning' },
    );
    const credential = await client.resetPassword(row.employeeId);
    temporaryPassword.value = credential.temporaryPassword;
    credentialDialogVisible.value = true;
  } catch {
    // The confirmation dialog is cancellable.
  }
}

async function handleDelete(row: EmployeeRecord) {
  try {
    await ElMessageBox.confirm(`确定要删除员工"${row.actualName}"吗？`, '删除确认', {
      type: 'warning',
    });
    await client.delete([row.employeeId]);
    ElMessage.success('删除成功');
    await getData();
  } catch {
    // The confirmation dialog is cancellable.
  }
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择要删除的员工');
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 名员工吗？`,
      '批量删除确认',
      { type: 'warning' },
    );
    await client.delete(selectedRows.value.map((row) => row.employeeId));
    selectedRows.value = [];
    ElMessage.success('批量删除成功');
    await getData();
  } catch {
    // The confirmation dialog is cancellable.
  }
}

function handleOpenBatchTransfer() {
  if (!selectedRows.value.length) {
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
    await client.assignDepartment({
      departmentId: transferDepartmentId.value,
      employeeIds: selectedRows.value.map((row) => row.employeeId),
    });
    selectedRows.value = [];
    transferDialogVisible.value = false;
    ElMessage.success('批量转移部门成功');
    await getData();
  } finally {
    transferLoading.value = false;
  }
}

function handleSelectionChange(rows: EmployeeRecord[]) {
  selectedRows.value = rows;
}

async function handleCopyPassword() {
  await navigator.clipboard.writeText(temporaryPassword.value);
  ElMessage.success('密码已复制');
}

onMounted(() => void getData());
defineExpose({ reload });
</script>

<template>
  <div class="employee-table-panel">
    <ElCard v-show="showSearchBar" class="employee-table-panel__search-card" shadow="never">
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
        <ElFormItem
          v-if="showDepartmentFilter"
          class="employee-table-panel__department-item"
          label="所属部门"
        >
          <ElSelect
            v-model="searchParamsProxy.departmentId"
            clearable
            filterable
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
          <ElSelect v-model="searchParamsProxy.disabled" clearable>
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
          @search="showSearchBar = !showSearchBar"
        >
          <template #left>
            <ElSpace>
              <AccessControl :codes="['identity.employee.create']" type="code">
                <ElButton type="primary" @click="emit('add')">新增员工</ElButton>
              </AccessControl>
              <AccessControl :codes="['identity.employee.delete']" type="code">
                <ElButton :disabled="!selectedRows.length" @click="handleBatchDelete">
                  批量删除
                </ElButton>
              </AccessControl>
              <AccessControl
                :codes="['identity.employee.department.assign']"
                type="code"
              >
                <ElButton
                  :disabled="!selectedRows.length"
                  @click="handleOpenBatchTransfer"
                >
                  批量转移部门
                </ElButton>
              </AccessControl>
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
              <ElAvatar :size="28" :src="row.avatar || undefined">
                {{ getAvatarText(row) }}
              </ElAvatar>
              <span class="employee-table-panel__primary">{{ row.actualName || '-' }}</span>
            </div>
          </template>
          <template #loginName="{ row }">
            <span class="employee-table-panel__secondary">{{ row.loginName || '-' }}</span>
          </template>
          <template #positionName="{ row }">
            <span>{{ formatPositionName(row) }}</span>
          </template>
          <template #disabledFlag="{ row }">
            <ElTag :type="row.disabledFlag ? 'info' : 'success'" effect="light" size="small">
              {{ row.disabledFlag ? '停用' : '启用' }}
            </ElTag>
          </template>
          <template #actions="{ row }">
            <ElSpace class="employee-table-panel__actions">
              <AccessControl :codes="['identity.employee.update']" type="code">
                <ElButton link size="small" type="primary" @click="emit('edit', row)">
                  编辑
                </ElButton>
              </AccessControl>
              <AccessControl
                :codes="[
                  row.disabledFlag
                    ? 'identity.employee.enable'
                    : 'identity.employee.disable',
                ]"
                type="code"
              >
                <ElButton link size="small" @click="handleToggleStatus(row)">
                  {{ row.disabledFlag ? '启用' : '停用' }}
                </ElButton>
              </AccessControl>
              <AccessControl
                :codes="['identity.employee.password.reset']"
                type="code"
              >
                <ElButton link size="small" @click="handleResetPassword(row)">
                  重置密码
                </ElButton>
              </AccessControl>
              <AccessControl :codes="['identity.employee.delete']" type="code">
                <ElButton link size="small" type="danger" @click="handleDelete(row)">
                  删除
                </ElButton>
              </AccessControl>
            </ElSpace>
          </template>
        </ArtTable>
      </ArtTablePanel>
    </ElCard>

    <ElDialog v-model="transferDialogVisible" title="批量转移部门" width="420px">
      <ElFormItem label="目标部门">
        <ElSelect v-model="transferDepartmentId" filterable style="width: 100%">
          <ElOption
            v-for="department in departmentOptions"
            :key="department.departmentId"
            :label="department.departmentName"
            :value="department.departmentId"
          />
        </ElSelect>
      </ElFormItem>
      <template #footer>
        <ElSpace>
          <ElButton @click="transferDialogVisible = false">取消</ElButton>
          <ElButton :loading="transferLoading" type="primary" @click="handleBatchTransferDepartment">
            确定转移
          </ElButton>
        </ElSpace>
      </template>
    </ElDialog>

    <ElDialog v-model="credentialDialogVisible" title="一次性密码" width="420px">
      <div class="credential-value">{{ temporaryPassword }}</div>
      <template #footer>
        <ElSpace>
          <ElButton @click="handleCopyPassword">复制密码</ElButton>
          <ElButton type="primary" @click="credentialDialogVisible = false">关闭</ElButton>
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
  flex-shrink: 0;
}

.employee-table-panel__table-card {
  border: 1px solid var(--el-border-color-lighter);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.employee-table-panel__table-card :deep(.el-card__body) {
  height: 100%;
  min-height: 0;
  padding: 16px;
}

.employee-table-panel :deep(.art-search-panel .el-form-item) {
  margin-bottom: 0;
}

.employee-table-panel :deep(.art-search-panel .el-input),
.employee-table-panel :deep(.art-search-panel .el-select) {
  width: 100%;
}

.employee-table-panel :deep(.employee-table-panel__keyword-item .el-form-item__content) {
  width: 216px;
}

.employee-table-panel :deep(.employee-table-panel__department-item .el-form-item__content),
.employee-table-panel :deep(.employee-table-panel__status-item .el-form-item__content) {
  width: 168px;
}

.employee-table-panel :deep(.art-table-panel),
.employee-table-panel :deep(.art-table) {
  min-height: 0;
}

.employee-table-panel__employee {
  align-items: center;
  display: inline-flex;
  gap: 10px;
}

.employee-table-panel__primary {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.employee-table-panel__secondary {
  color: var(--el-text-color-secondary);
}

.employee-table-panel__actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.employee-table-panel__actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.employee-table-panel__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.credential-value {
  background: var(--el-color-primary-light-9);
  border-radius: 4px;
  color: var(--el-color-primary);
  font-family: 'Courier New', monospace;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 2px;
  padding: 14px;
  text-align: center;
}
</style>
