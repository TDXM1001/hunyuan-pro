<script setup lang="ts">
import type {
  DepartmentCommand,
  DepartmentRecord,
  OrganizationMember,
} from './contract';
import type { FormInstance, FormRules } from 'element-plus';

import { computed, inject, onMounted, reactive, ref } from 'vue';

import { AccessControl } from '@vben/access';
import { Page } from '@vben/common-ui';

import {
  ElButton,
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
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import { organizationDepartmentClientKey } from './dependencies';

defineOptions({ name: 'OrganizationDepartmentDirectory' });

interface DepartmentRow extends DepartmentRecord {
  children?: DepartmentRow[];
}

interface FormModel extends DepartmentCommand {
  departmentId?: number;
}

const injectedClient = inject(organizationDepartmentClientKey);
if (!injectedClient) {
  throw new Error('organization department client is not registered');
}
const client = injectedClient;

const loading = ref(false);
const keyword = ref('');
const rows = ref<DepartmentRecord[]>([]);
const managers = ref<OrganizationMember[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'update'>('create');
const formRef = ref<FormInstance>();
const form = reactive<FormModel>({
  departmentName: '',
  managerId: null,
  parentId: 0,
  sort: 100,
});

const rules: FormRules<FormModel> = {
  departmentName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'change' }],
};

const treeRows = computed(() => buildTree(rows.value));
const parentOptions = computed(() => rows.value);
const filteredRows = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  return value
    ? rows.value.filter((item) =>
        [item.departmentName, item.managerName]
          .filter(Boolean)
          .some((field) => field!.toLowerCase().includes(value)),
      )
    : treeRows.value;
});

function buildTree(items: DepartmentRecord[]) {
  const nodeMap = new Map<number, DepartmentRow>();
  items.forEach((item) => nodeMap.set(item.departmentId, { ...item, children: [] }));
  const roots: DepartmentRow[] = [];
  nodeMap.forEach((node) => {
    const parent = nodeMap.get(node.parentId ?? 0);
    if (parent && parent.departmentId !== node.departmentId) {
      parent.children!.push(node);
    } else {
      roots.push(node);
    }
  });
  return roots;
}

async function load() {
  loading.value = true;
  try {
    const [departments, managerOptions] = await Promise.all([
      client.list(),
      client.listManagers(),
    ]);
    rows.value = departments;
    managers.value = managerOptions;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  Object.assign(form, {
    departmentId: undefined,
    departmentName: '',
    managerId: null,
    parentId: 0,
    sort: 100,
  });
}

function openCreate(parentId = 0) {
  resetForm();
  dialogMode.value = 'create';
  form.parentId = parentId;
  dialogVisible.value = true;
}

function openUpdate(rawRow: unknown) {
  const row = rawRow as DepartmentRecord;
  dialogMode.value = 'update';
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  const command: DepartmentCommand = {
    departmentName: form.departmentName,
    managerId: form.managerId,
    parentId: form.parentId,
    sort: form.sort,
  };
  if (dialogMode.value === 'create') {
    await client.create(command);
    ElMessage.success('部门已创建');
  } else {
    await client.update(form.departmentId!, command);
    ElMessage.success('部门已更新');
  }
  dialogVisible.value = false;
  await load();
}

async function remove(rawRow: unknown) {
  const row = rawRow as DepartmentRecord;
  await ElMessageBox.confirm(`确定删除部门“${row.departmentName}”吗？`, '删除确认', {
    type: 'warning',
  });
  await client.delete(row.departmentId);
  ElMessage.success('部门已删除');
  await load();
}

onMounted(() => void load());
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <section class="directory-page">
      <header class="directory-page__toolbar">
        <ElInput v-model="keyword" clearable placeholder="搜索部门或负责人" />
        <AccessControl :codes="['organization.department.create']" type="code">
          <ElButton type="primary" @click="openCreate()">新增顶级部门</ElButton>
        </AccessControl>
      </header>

      <ElTable
        v-loading="loading"
        :data="filteredRows"
        default-expand-all
        height="100%"
        row-key="departmentId"
        :tree-props="{ children: 'children' }"
      >
        <ElTableColumn label="部门名称" min-width="260" prop="departmentName">
          <template #default="{ row }">
            <ElSpace>
              <span class="directory-page__name">{{ row.departmentName }}</span>
              <ElTag v-if="!row.parentId" effect="plain" size="small">根节点</ElTag>
            </ElSpace>
          </template>
        </ElTableColumn>
        <ElTableColumn label="负责人" min-width="140" prop="managerName">
          <template #default="{ row }">{{ row.managerName || '-' }}</template>
        </ElTableColumn>
        <ElTableColumn align="center" label="排序" prop="sort" width="90" />
        <ElTableColumn label="更新时间" min-width="180" prop="updateTime" />
        <ElTableColumn align="center" fixed="right" label="操作" width="230">
          <template #default="{ row }">
            <ElSpace>
              <AccessControl :codes="['organization.department.create']" type="code">
                <ElButton link type="primary" @click="openCreate(row.departmentId)">新增下级</ElButton>
              </AccessControl>
              <AccessControl :codes="['organization.department.update']" type="code">
                <ElButton link type="primary" @click="openUpdate(row)">编辑</ElButton>
              </AccessControl>
              <AccessControl :codes="['organization.department.delete']" type="code">
                <ElButton link type="danger" @click="remove(row)">删除</ElButton>
              </AccessControl>
            </ElSpace>
          </template>
        </ElTableColumn>
      </ElTable>
    </section>

    <ElDialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增部门' : '编辑部门'" width="520px" @closed="resetForm">
      <ElForm ref="formRef" :model="form" :rules="rules" label-position="top">
        <ElFormItem label="部门名称" prop="departmentName">
          <ElInput v-model="form.departmentName" maxlength="50" show-word-limit />
        </ElFormItem>
        <ElFormItem label="上级部门" prop="parentId">
          <ElSelect v-model="form.parentId" filterable>
            <ElOption label="顶级部门" :value="0" />
            <ElOption
              v-for="item in parentOptions"
              :key="item.departmentId"
              :disabled="item.departmentId === form.departmentId"
              :label="item.departmentName"
              :value="item.departmentId"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="部门负责人" prop="managerId">
          <ElSelect v-model="form.managerId" clearable filterable placeholder="请选择负责人">
            <ElOption
              v-for="manager in managers"
              :key="manager.employeeId"
              :label="manager.actualName"
              :value="manager.employeeId"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="排序" prop="sort">
          <ElInputNumber v-model="form.sort" :min="0" controls-position="right" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElSpace>
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="submit">保存</ElButton>
        </ElSpace>
      </template>
    </ElDialog>
  </Page>
</template>

<style scoped>
.directory-page {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.directory-page__toolbar {
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.directory-page__toolbar :deep(.el-input) {
  max-width: 320px;
}

.directory-page__name {
  font-weight: 600;
}

@media (width <= 768px) {
  .directory-page__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .directory-page__toolbar :deep(.el-input) {
    max-width: none;
  }
}
</style>
