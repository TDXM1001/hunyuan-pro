<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus';

import type {
  AccessCapabilityNode,
  AccessDataScopeDefinition,
  AccessRoleCommand,
  AccessRoleMember,
  AccessRoleRecord,
} from '../contract';

import { computed, inject, onMounted, reactive, ref, watch } from 'vue';

import { AccessControl } from '@vben/access';
import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  type ColumnOption,
  useTableColumns,
} from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';

import { User } from '@element-plus/icons-vue';
import {
  ElButton,
  ElCard,
  ElCheckbox,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElMessage,
  ElPopconfirm,
  ElRadioButton,
  ElRadioGroup,
  ElScrollbar,
  ElSpace,
  ElTabPane,
  ElTabs,
} from 'element-plus';

import { accessClientKey } from '../dependencies';

defineOptions({ name: 'AccessRoleManagement' });

interface RoleFormModel extends AccessRoleCommand {
  roleId?: number;
}

interface PermissionRow {
  actions: AccessCapabilityNode[];
  depth: number;
  node: AccessCapabilityNode;
}

const injectedAccessClient = inject(accessClientKey);
if (!injectedAccessClient) {
  throw new Error('访问控制客户端未注入');
}
const accessClient = injectedAccessClient;

const loading = ref(false);
const permissionLoading = ref(false);
const permissionSaving = ref(false);
const dataScopeLoading = ref(false);
const dataScopeSaving = ref(false);
const employeeLoading = ref(false);
const candidateLoading = ref(false);
const allRows = ref<AccessRoleRecord[]>([]);
const activeRoleId = ref<number>();
const activeTab = ref<'dataScope' | 'employees' | 'permissions'>('permissions');
const menuTreeList = ref<AccessCapabilityNode[]>([]);
const selectedMenuIds = ref<number[]>([]);
const dataScopes = ref<AccessDataScopeDefinition[]>([]);
const roleDataScopeValues = reactive<Record<number, number>>({});
const employeeRows = ref<AccessRoleMember[]>([]);
const selectedEmployeeIds = ref<number[]>([]);
const employeeKeyword = ref('');
const candidateRows = ref<AccessRoleMember[]>([]);
const selectedCandidateIds = ref<number[]>([]);
const candidateKeyword = ref('');
const dialogVisible = ref(false);
const employeeDialogVisible = ref(false);
const formLoading = ref(false);
const formRef = ref<FormInstance>();
const employeePagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});
const candidatePagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const formData = reactive<RoleFormModel>({
  remark: '',
  roleCode: '',
  roleName: '',
});

const rules: FormRules<RoleFormModel> = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
};

const currentRole = computed(() =>
  allRows.value.find((item) => item.roleId === activeRoleId.value),
);

const parentIdMap = computed(() => {
  const map = new Map<number, null | number>();

  function visit(nodes: AccessCapabilityNode[]) {
    nodes.forEach((node) => {
      map.set(node.capabilityId, node.parentId ?? null);
      visit(node.children ?? []);
    });
  }

  visit(menuTreeList.value);
  return map;
});

const permissionRows = computed<PermissionRow[]>(() => {
  const rows: PermissionRow[] = [];

  function visit(nodes: AccessCapabilityNode[], depth: number) {
    nodes
      .filter((node) => node.capabilityType !== 3)
      .forEach((node) => {
        const children = node.children ?? [];
        rows.push({
          actions: children.filter((child) => child.capabilityType === 3),
          depth,
          node,
        });
        visit(
          children.filter((child) => child.capabilityType !== 3),
          depth + 1,
        );
      });
  }

  visit(menuTreeList.value, 0);
  return rows;
});

const employeeColumnsFactory = (): ColumnOption<AccessRoleMember>[] => [
  { type: 'selection', width: 50, align: 'center' },
  { prop: 'actualName', label: '员工姓名', minWidth: 120 },
  { prop: 'loginName', label: '登录账号', minWidth: 120 },
  { prop: 'departmentName', label: '部门', minWidth: 120 },
  { prop: 'phone', label: '手机号', minWidth: 132 },
  {
    prop: 'actions',
    label: '操作',
    width: 96,
    align: 'center',
    fixed: 'right',
    useSlot: true,
  },
];

const candidateColumnsFactory = (): ColumnOption<AccessRoleMember>[] => [
  { type: 'selection', width: 50, align: 'center' },
  { prop: 'actualName', label: '员工姓名', minWidth: 120 },
  { prop: 'loginName', label: '登录账号', minWidth: 120 },
  { prop: 'departmentName', label: '部门', minWidth: 120 },
  { prop: 'phone', label: '手机号', minWidth: 132 },
];

const { columns: employeeColumns, columnChecks: employeeColumnChecks } =
  useTableColumns(employeeColumnsFactory);
const { columns: candidateColumns, columnChecks: candidateColumnChecks } =
  useTableColumns(candidateColumnsFactory);

function resetForm() {
  Object.assign(formData, {
    remark: '',
    roleCode: '',
    roleName: '',
  });
  formData.roleId = undefined;
}

function normalizeSelectedMenuIds(ids: number[]) {
  return [...new Set(ids)].sort((first, second) => first - second);
}

function resetRoleDataScopeValues(values: Record<number, number>) {
  Object.keys(roleDataScopeValues).forEach((key) => {
    delete roleDataScopeValues[Number(key)];
  });
  Object.assign(roleDataScopeValues, values);
}

function collectDescendantIds(node: AccessCapabilityNode): number[] {
  return [
    node.capabilityId,
    ...(node.children ?? []).flatMap((child) => collectDescendantIds(child)),
  ];
}

function collectAncestorIds(menuId: number): number[] {
  const ids: number[] = [];
  let parentId = parentIdMap.value.get(menuId);

  while (parentId && parentId > 0) {
    ids.push(parentId);
    parentId = parentIdMap.value.get(parentId);
  }

  return ids;
}

function isNodeChecked(node: AccessCapabilityNode) {
  return selectedMenuIds.value.includes(node.capabilityId);
}

function isNodeIndeterminate(node: AccessCapabilityNode) {
  const descendantIds = collectDescendantIds(node).filter(
    (menuId) => menuId !== node.capabilityId,
  );

  if (descendantIds.length === 0) {
    return false;
  }

  const checkedCount = descendantIds.filter((menuId) =>
    selectedMenuIds.value.includes(menuId),
  ).length;

  return checkedCount > 0 && checkedCount < descendantIds.length;
}

function handleNodeCheck(node: AccessCapabilityNode, checked: boolean) {
  const selected = new Set(selectedMenuIds.value);

  if (checked) {
    collectDescendantIds(node).forEach((menuId) => selected.add(menuId));
    collectAncestorIds(node.capabilityId).forEach((menuId) =>
      selected.add(menuId),
    );
  } else {
    collectDescendantIds(node).forEach((menuId) => selected.delete(menuId));
  }

  selectedMenuIds.value = normalizeSelectedMenuIds([...selected]);
}

async function loadRolePermissions(roleId: number) {
  permissionLoading.value = true;
  try {
    const result = await accessClient.getRoleCapabilities(roleId);
    menuTreeList.value = result.capabilityTree ?? [];
    selectedMenuIds.value = normalizeSelectedMenuIds(
      result.selectedCapabilityIds ?? [],
    );
  } finally {
    permissionLoading.value = false;
  }
}

async function loadRoleDataScopes(roleId: number) {
  dataScopeLoading.value = true;
  try {
    const [scopeList, selectedList] = await Promise.all([
      dataScopes.value.length > 0
        ? dataScopes.value
        : accessClient.listDataScopes(),
      accessClient.getRoleDataScopes(roleId),
    ]);
    dataScopes.value = scopeList ?? [];

    const selectedMap = new Map(
      (selectedList.dataScopes ?? []).map((item) => [
        item.dataScopeType,
        item.viewType,
      ]),
    );
    resetRoleDataScopeValues(
      Object.fromEntries(
        dataScopes.value.map((scope) => [
          scope.dataScopeType,
          selectedMap.get(scope.dataScopeType) ??
            scope.viewOptions[0]?.viewType ??
            0,
        ]),
      ),
    );
  } finally {
    dataScopeLoading.value = false;
  }
}

async function loadRoleEmployees(roleId: number) {
  employeeLoading.value = true;
  try {
    const result = await accessClient.queryRoleMembers(roleId, {
      keywords: employeeKeyword.value,
      pageNum: employeePagination.current,
      pageSize: employeePagination.size,
    });
    employeeRows.value = result.list ?? [];
    employeePagination.total = result.total ?? 0;
  } finally {
    employeeLoading.value = false;
  }
}

async function loadCandidateEmployees() {
  if (!activeRoleId.value) {
    candidateRows.value = [];
    candidatePagination.total = 0;
    return;
  }

  candidateLoading.value = true;
  try {
    const result = await accessClient.queryRoleMemberCandidates(
      activeRoleId.value,
      {
        keywords: candidateKeyword.value,
        pageNum: candidatePagination.current,
        pageSize: candidatePagination.size,
      },
    );
    candidateRows.value = result.list ?? [];
    candidatePagination.total = result.total ?? 0;
  } finally {
    candidateLoading.value = false;
  }
}

async function loadRoles() {
  loading.value = true;
  try {
    allRows.value = (await accessClient.listRoles()) ?? [];
    if (!allRows.value.some((item) => item.roleId === activeRoleId.value)) {
      activeRoleId.value = allRows.value[0]?.roleId;
    }
  } finally {
    loading.value = false;
  }
}

function openAddDialog() {
  resetForm();
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  formLoading.value = true;
  try {
    await accessClient.createRole(formData);
    ElMessage.success('新增角色成功');
    dialogVisible.value = false;
    await loadRoles();
  } finally {
    formLoading.value = false;
  }
}

async function handleSavePermissions() {
  if (!activeRoleId.value) {
    return;
  }

  permissionSaving.value = true;
  try {
    await accessClient.replaceRoleCapabilities(
      activeRoleId.value,
      normalizeSelectedMenuIds(selectedMenuIds.value),
    );
    ElMessage.success('权限保存成功');
    await loadRolePermissions(activeRoleId.value);
  } finally {
    permissionSaving.value = false;
  }
}

async function handleSaveDataScopes() {
  if (!activeRoleId.value || dataScopes.value.length === 0) {
    return;
  }

  dataScopeSaving.value = true;
  try {
    await accessClient.replaceRoleDataScopes(
      activeRoleId.value,
      dataScopes.value.map((scope) => ({
        dataScopeType: scope.dataScopeType,
        viewType:
          roleDataScopeValues[scope.dataScopeType] ??
          scope.viewOptions[0]?.viewType ??
          0,
      })),
    );
    ElMessage.success('数据范围保存成功');
    await loadRoleDataScopes(activeRoleId.value);
  } finally {
    dataScopeSaving.value = false;
  }
}

function handleHeaderSave() {
  if (activeTab.value === 'dataScope') {
    void handleSaveDataScopes();
    return;
  }
  void handleSavePermissions();
}

function handleEmployeeSelectionChange(rows: AccessRoleMember[]) {
  selectedEmployeeIds.value = rows.map((row) => row.employeeId);
}

function handleCandidateSelectionChange(rows: AccessRoleMember[]) {
  selectedCandidateIds.value = rows.map((row) => row.employeeId);
}

function handleEmployeeSearch() {
  employeePagination.current = 1;
  if (activeRoleId.value) {
    void loadRoleEmployees(activeRoleId.value);
  }
}

function handleEmployeeSizeChange(size: number) {
  employeePagination.size = size;
  employeePagination.current = 1;
  if (activeRoleId.value) {
    void loadRoleEmployees(activeRoleId.value);
  }
}

function handleEmployeeCurrentChange(current: number) {
  employeePagination.current = current;
  if (activeRoleId.value) {
    void loadRoleEmployees(activeRoleId.value);
  }
}

function openEmployeeDialog() {
  selectedCandidateIds.value = [];
  candidateKeyword.value = '';
  candidatePagination.current = 1;
  employeeDialogVisible.value = true;
  void loadCandidateEmployees();
}

function handleCandidateSearch() {
  candidatePagination.current = 1;
  void loadCandidateEmployees();
}

function handleCandidateSizeChange(size: number) {
  candidatePagination.size = size;
  candidatePagination.current = 1;
  void loadCandidateEmployees();
}

function handleCandidateCurrentChange(current: number) {
  candidatePagination.current = current;
  void loadCandidateEmployees();
}

async function handleAddRoleEmployees() {
  if (!activeRoleId.value || selectedCandidateIds.value.length === 0) {
    ElMessage.warning('请选择要添加的员工');
    return;
  }

  await accessClient.assignRoleMembers(
    activeRoleId.value,
    selectedCandidateIds.value,
  );
  ElMessage.success('员工添加成功');
  employeeDialogVisible.value = false;
  await loadRoleEmployees(activeRoleId.value);
}

async function handleRemoveRoleEmployee(employeeId: number) {
  if (!activeRoleId.value) {
    return;
  }

  await accessClient.removeRoleMembers(activeRoleId.value, [employeeId]);
  ElMessage.success('员工移除成功');
  await loadRoleEmployees(activeRoleId.value);
}

async function handleBatchRemoveRoleEmployees() {
  if (!activeRoleId.value || selectedEmployeeIds.value.length === 0) {
    ElMessage.warning('请选择要移除的员工');
    return;
  }

  await accessClient.removeRoleMembers(
    activeRoleId.value,
    selectedEmployeeIds.value,
  );
  ElMessage.success('员工移除成功');
  selectedEmployeeIds.value = [];
  await loadRoleEmployees(activeRoleId.value);
}

watch(activeRoleId, (roleId) => {
  if (!roleId) {
    menuTreeList.value = [];
    selectedMenuIds.value = [];
    employeeRows.value = [];
    resetRoleDataScopeValues({});
    return;
  }

  void loadRolePermissions(roleId).catch((error) => {
    ElMessage.error(error?.message || '角色权限加载失败');
  });
  if (activeTab.value === 'dataScope') {
    void loadRoleDataScopes(roleId).catch((error) => {
      ElMessage.error(error?.message || '数据范围加载失败');
    });
  }
  if (activeTab.value === 'employees') {
    void loadRoleEmployees(roleId).catch((error) => {
      ElMessage.error(error?.message || '角色员工加载失败');
    });
  }
});

watch(activeTab, (tab) => {
  if (!activeRoleId.value) {
    return;
  }

  if (tab === 'dataScope') {
    void loadRoleDataScopes(activeRoleId.value).catch((error) => {
      ElMessage.error(error?.message || '数据范围加载失败');
    });
  }
  if (tab === 'employees') {
    void loadRoleEmployees(activeRoleId.value).catch((error) => {
      ElMessage.error(error?.message || '角色员工加载失败');
    });
  }
});

onMounted(() => {
  void loadRoles().catch((error) => {
    ElMessage.error(error?.message || '角色数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="role-page">
      <ElCard class="role-page__role-card" shadow="never">
        <div class="role-page__role-header">
          <div class="role-page__role-title">角色列表</div>
          <AccessControl :codes="['access.role.create']" type="code">
            <ElButton type="primary" @click="openAddDialog">添加</ElButton>
          </AccessControl>
        </div>

        <ElScrollbar class="role-page__role-scrollbar">
          <div class="role-page__role-list">
            <button
              v-for="role in allRows"
              :key="role.roleId"
              class="role-page__role-item"
              :class="{
                'role-page__role-item--active': role.roleId === activeRoleId,
              }"
              type="button"
              @click="activeRoleId = role.roleId"
            >
              <ElIcon class="role-page__role-icon">
                <User />
              </ElIcon>
              <span class="role-page__role-name">{{ role.roleName }}</span>
            </button>
          </div>

          <ElEmpty
            v-if="!loading && allRows.length === 0"
            description="暂无角色"
            :image-size="96"
          />
        </ElScrollbar>
      </ElCard>

      <ElCard class="role-page__permission-card" shadow="never">
        <div class="role-page__permission-header">
          <div>
            <div class="role-page__permission-title">
              {{ currentRole?.roleName || '请选择角色' }}
            </div>
            <div class="role-page__permission-subtitle">
              {{
                currentRole?.remark ||
                currentRole?.roleCode ||
                '选择左侧角色后配置权限'
              }}
            </div>
          </div>
          <AccessControl
            v-if="activeTab !== 'employees'"
            :codes="[
              activeTab === 'dataScope'
                ? 'access.data-scope.update'
                : 'access.capability.grant',
            ]"
            type="code"
          >
            <ElButton
              :disabled="!activeRoleId"
              :loading="
                activeTab === 'dataScope' ? dataScopeSaving : permissionSaving
              "
              type="primary"
              @click="handleHeaderSave"
            >
              保存
            </ElButton>
          </AccessControl>
        </div>

        <ElTabs v-model="activeTab" class="role-page__tabs">
          <ElTabPane label="角色-功能权限" name="permissions">
            <div
              v-loading="permissionLoading"
              class="role-page__permission-matrix"
            >
              <div class="role-page__matrix-head">
                <span>模块 / 功能</span>
                <span>权限项</span>
              </div>

              <ElScrollbar class="role-page__matrix-scrollbar">
                <div
                  v-for="row in permissionRows"
                  :key="row.node.capabilityId"
                  class="role-page__matrix-row"
                >
                  <div class="role-page__matrix-module">
                    <span
                      class="role-page__matrix-indent"
                      :style="{ width: `${row.depth * 18}px` }"
                    ></span>
                    <ElCheckbox
                      :indeterminate="isNodeIndeterminate(row.node)"
                      :model-value="isNodeChecked(row.node)"
                      @change="handleNodeCheck(row.node, Boolean($event))"
                    >
                      {{ row.node.capabilityName }}
                    </ElCheckbox>
                  </div>

                  <div class="role-page__matrix-actions">
                    <ElCheckbox
                      v-for="action in row.actions"
                      :key="action.capabilityId"
                      :model-value="isNodeChecked(action)"
                      @change="handleNodeCheck(action, Boolean($event))"
                    >
                      {{ action.capabilityName }}
                    </ElCheckbox>
                    <span
                      v-if="row.actions.length === 0"
                      class="role-page__matrix-muted"
                    >
                      菜单访问
                    </span>
                  </div>
                </div>

                <ElEmpty
                  v-if="!permissionLoading && permissionRows.length === 0"
                  description="暂无权限菜单"
                  :image-size="112"
                />
              </ElScrollbar>
            </div>
          </ElTabPane>

          <ElTabPane label="角色-数据范围" name="dataScope">
            <div
              v-loading="dataScopeLoading"
              class="role-page__data-scope-panel"
            >
              <div
                v-for="scope in dataScopes"
                :key="scope.dataScopeType"
                class="role-page__data-scope-row"
              >
                <div class="role-page__data-scope-info">
                  <div class="role-page__data-scope-title">
                    {{ scope.dataScopeTypeName }}
                  </div>
                  <div class="role-page__data-scope-desc">
                    {{ scope.dataScopeTypeDescription }}
                  </div>
                </div>
                <ElRadioGroup
                  v-model="roleDataScopeValues[scope.dataScopeType]"
                >
                  <ElRadioButton
                    v-for="viewType in scope.viewOptions"
                    :key="viewType.viewType"
                    :value="viewType.viewType"
                  >
                    {{ viewType.name }}
                  </ElRadioButton>
                </ElRadioGroup>
              </div>

              <ElEmpty
                v-if="!dataScopeLoading && dataScopes.length === 0"
                description="暂无数据范围配置"
                :image-size="112"
              />
            </div>
          </ElTabPane>

          <ElTabPane label="角色-员工列表" name="employees">
            <div class="role-page__employee-panel">
              <ArtTablePanel>
                <ArtTableHeader
                  v-model="employeeColumnChecks"
                  :loading="employeeLoading"
                  layout="size,fullscreen,columns,settings"
                >
                  <template #left>
                    <div class="role-page__employee-toolbar">
                      <ElInput
                        v-model="employeeKeyword"
                        clearable
                        placeholder="员工姓名、登录账号、手机号"
                        @keyup.enter="handleEmployeeSearch"
                      />
                      <ElButton type="primary" @click="handleEmployeeSearch">
                        查询
                      </ElButton>
                      <AccessControl
                        :codes="['access.role.employee.assign']"
                        type="code"
                      >
                        <ElButton @click="openEmployeeDialog">
添加员工
</ElButton>
                      </AccessControl>
                      <AccessControl
                        :codes="['access.role.employee.remove']"
                        type="code"
                      >
                        <ElButton
                          :disabled="selectedEmployeeIds.length === 0"
                          type="danger"
                          @click="handleBatchRemoveRoleEmployees"
                        >
                          批量移除
                        </ElButton>
                      </AccessControl>
                    </div>
                  </template>
                </ArtTableHeader>

                <ArtTable
                  :columns="employeeColumns"
                  :data="employeeRows"
                  height="100%"
                  :loading="employeeLoading"
                  :pagination="employeePagination"
                  :pagination-options="{
                    align: 'center',
                    hideOnSinglePage: false,
                    layout: 'sizes, prev, pager, next, jumper',
                    pageSizes: [10, 20, 30],
                    showTotalSummary: true,
                    size: 'small',
                  }"
                  row-key="employeeId"
                  @pagination:current-change="handleEmployeeCurrentChange"
                  @pagination:size-change="handleEmployeeSizeChange"
                  @selection-change="handleEmployeeSelectionChange"
                >
                  <template #actions="{ row }">
                    <div class="role-page__employee-actions">
                      <AccessControl
                        :codes="['access.role.employee.remove']"
                        type="code"
                      >
                        <ElPopconfirm
                          title="确定从该角色移除此员工吗？"
                          @confirm="handleRemoveRoleEmployee(row.employeeId)"
                        >
                          <template #reference>
                            <ElButton link size="small" type="danger">
                              移除
                            </ElButton>
                          </template>
                        </ElPopconfirm>
                      </AccessControl>
                    </div>
                  </template>
                </ArtTable>
              </ArtTablePanel>
            </div>
          </ElTabPane>
        </ElTabs>
      </ElCard>
    </div>

    <ElDialog
      v-model="dialogVisible"
      title="新增角色"
      width="520px"
      @closed="resetForm"
    >
      <ElForm
        ref="formRef"
        v-loading="formLoading"
        :model="formData"
        :rules="rules"
        label-position="top"
      >
        <ElFormItem label="角色名称" prop="roleName">
          <ElInput v-model="formData.roleName" placeholder="请输入角色名称" />
        </ElFormItem>
        <ElFormItem label="角色编码" prop="roleCode">
          <ElInput v-model="formData.roleCode" placeholder="请输入角色编码" />
        </ElFormItem>
        <ElFormItem label="角色备注" prop="remark">
          <ElInput
            v-model="formData.remark"
            maxlength="255"
            placeholder="请输入角色备注"
            type="textarea"
          />
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElSpace>
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton :loading="formLoading" type="primary" @click="handleSubmit">
            保存
          </ElButton>
        </ElSpace>
      </template>
    </ElDialog>

    <ElDialog v-model="employeeDialogVisible" title="添加员工" width="760px">
      <div class="role-page__candidate-panel">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="candidateColumnChecks"
            :loading="candidateLoading"
            layout="size,columns,settings"
          >
            <template #left>
              <div class="role-page__employee-toolbar">
                <ElInput
                  v-model="candidateKeyword"
                  clearable
                  placeholder="员工姓名、登录账号、手机号"
                  @keyup.enter="handleCandidateSearch"
                />
                <ElButton type="primary" @click="handleCandidateSearch">
                  查询
                </ElButton>
              </div>
            </template>
          </ArtTableHeader>

          <ArtTable
            :columns="candidateColumns"
            :data="candidateRows"
            height="360"
            :loading="candidateLoading"
            :pagination="candidatePagination"
            :pagination-options="{
              align: 'center',
              hideOnSinglePage: false,
              layout: 'sizes, prev, pager, next, jumper',
              pageSizes: [10, 20, 30],
              showTotalSummary: true,
              size: 'small',
            }"
            row-key="employeeId"
            @pagination:current-change="handleCandidateCurrentChange"
            @pagination:size-change="handleCandidateSizeChange"
            @selection-change="handleCandidateSelectionChange"
          />
        </ArtTablePanel>
      </div>

      <template #footer>
        <ElSpace>
          <ElButton @click="employeeDialogVisible = false">取消</ElButton>
          <AccessControl :codes="['access.role.employee.assign']" type="code">
            <ElButton type="primary" @click="handleAddRoleEmployees">
              添加
            </ElButton>
          </AccessControl>
        </ElSpace>
      </template>
    </ElDialog>
  </Page>
</template>

<style scoped>
.role-page {
  display: grid;
  gap: 12px;
  grid-template-columns: 248px minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.role-page__role-card,
.role-page__permission-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  min-height: 0;
  overflow: hidden;
}

.role-page__role-card :deep(.el-card__body),
.role-page__permission-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 16px;
}

.role-page__role-header,
.role-page__permission-header {
  align-items: center;
  display: flex;
  flex-shrink: 0;
  justify-content: space-between;
}

.role-page__role-header {
  min-height: 58px;
  padding: 10px 12px;
}

.role-page__permission-header {
  margin-bottom: 14px;
}

.role-page__role-title,
.role-page__permission-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.role-page__permission-subtitle {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 22px;
  margin-top: 2px;
}

.role-page__role-scrollbar,
.role-page__matrix-scrollbar {
  flex: 1;
  min-height: 0;
}

.role-page__role-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 18px;
}

.role-page__role-item {
  background: var(--el-bg-color);
  border: 0;
  border-radius: 6px;
  color: var(--el-text-color-primary);
  cursor: pointer;
  display: flex;
  gap: 10px;
  min-height: 54px;
  padding: 0 14px;
  position: relative;
  text-align: left;
  transition:
    background-color 0.16s ease,
    color 0.16s ease;
}

.role-page__role-item:hover {
  background: var(--el-fill-color-light);
}

.role-page__role-item--active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.role-page__role-item--active::before {
  background: var(--el-color-primary);
  border-radius: 999px;
  bottom: 8px;
  content: '';
  left: 0;
  position: absolute;
  top: 8px;
  width: 3px;
}

.role-page__role-icon {
  align-self: center;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
  font-size: 16px;
}

.role-page__role-item--active .role-page__role-icon {
  color: var(--el-color-primary);
}

.role-page__role-name {
  align-self: center;
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-page__tabs {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.role-page__tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
}

.role-page__tabs :deep(.el-tab-pane) {
  height: 100%;
  min-height: 0;
}

.role-page__permission-matrix {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.role-page__matrix-head,
.role-page__matrix-row {
  display: grid;
  grid-template-columns: minmax(260px, 32%) minmax(420px, 1fr);
}

.role-page__matrix-head {
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-regular);
  flex-shrink: 0;
  font-size: 13px;
  line-height: 22px;
}

.role-page__matrix-head span,
.role-page__matrix-module,
.role-page__matrix-actions {
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-lighter);
  display: flex;
  min-height: 44px;
  padding: 8px 12px;
}

.role-page__matrix-head span:first-child,
.role-page__matrix-module {
  border-right: 1px solid var(--el-border-color-lighter);
}

.role-page__matrix-actions {
  flex-wrap: wrap;
  gap: 8px 18px;
}

.role-page__matrix-actions :deep(.el-checkbox) {
  height: 28px;
  margin-right: 0;
}

.role-page__matrix-indent {
  flex-shrink: 0;
}

.role-page__matrix-muted {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
  line-height: 22px;
}

.role-page__data-scope-panel,
.role-page__employee-panel {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.role-page__data-scope-panel {
  display: flex;
  flex-direction: column;
}

.role-page__data-scope-row {
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-lighter);
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(220px, 28%) minmax(0, 1fr);
  min-height: 72px;
  padding: 12px 16px;
}

.role-page__data-scope-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
}

.role-page__data-scope-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 22px;
  margin-top: 2px;
}

.role-page__employee-panel {
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.role-page__employee-toolbar {
  align-items: center;
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.role-page__employee-toolbar :deep(.el-input) {
  width: 240px;
}

.role-page__employee-panel :deep(.art-table-panel),
.role-page__employee-panel :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.role-page__employee-panel :deep(.art-table-header) {
  margin-bottom: 18px;
}

.role-page__employee-panel :deep(.art-table) {
  --art-table-section-gap: 8px;
}

.role-page__employee-actions {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  justify-content: center;
}

.role-page__employee-actions :deep(.el-button) {
  font-size: 14px;
  line-height: 22px;
  min-height: 22px;
  padding: 0;
}

.role-page__employee-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.role-page__candidate-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.role-page__candidate-panel :deep(.art-table-panel),
.role-page__candidate-panel :deep(.art-table) {
  flex: 1;
  min-height: 0;
}

.role-page__candidate-panel :deep(.art-table-header) {
  margin-bottom: 18px;
}

.role-page__candidate-panel :deep(.art-table) {
  --art-table-section-gap: 8px;
}

@media (width <= 960px) {
  .role-page {
    grid-template-columns: 1fr;
    overflow: auto;
  }

  .role-page__role-card {
    min-height: 360px;
  }

  .role-page__permission-card {
    min-height: 620px;
  }

  .role-page__matrix-head,
  .role-page__matrix-row {
    grid-template-columns: minmax(220px, 36%) minmax(360px, 1fr);
    min-width: 720px;
  }
}
</style>
