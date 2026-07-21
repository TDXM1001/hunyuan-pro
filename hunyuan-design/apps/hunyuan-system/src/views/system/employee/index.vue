<script setup lang="ts">
import type {
  EmployeeRecord,
  PositionRecord,
  RoleRecord,
} from '#/api/system/organization';
import type { DepartmentRecord } from '@hunyuan/feature-organization';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { createOrganizationDepartmentClient } from '@hunyuan/feature-organization';

import { ElMessage } from 'element-plus';

import {
  listPositions,
  listRoles,
} from '#/api/system/organization';
import { requestClient } from '#/api/request';

import EmployeeForm from './components/employee-form.vue';
import EmployeeOrgTree from './components/employee-org-tree.vue';
import EmployeeTablePanel from './components/employee-table-panel.vue';

defineOptions({ name: 'SystemEmployeeIndex' });

const bootstrapping = ref(false);
const departments = ref<DepartmentRecord[]>([]);
const positions = ref<PositionRecord[]>([]);
const roles = ref<RoleRecord[]>([]);
const selectedDepartmentId = ref<null | number>(null);
const employeeTotal = ref(0);
const formVisible = ref(false);
const formMode = ref<'add' | 'edit'>('add');
const currentEmployee = ref<EmployeeRecord>();
const tablePanelRef = ref<InstanceType<typeof EmployeeTablePanel>>();
const organizationDepartmentClient = createOrganizationDepartmentClient(requestClient);

// 页面只负责装配基础字典和当前机构，表格查询由右侧组件自己维护。
async function bootstrap() {
  bootstrapping.value = true;
  try {
    const [departmentList, positionList, roleList] = await Promise.all([
      organizationDepartmentClient.list(),
      listPositions(),
      listRoles(),
    ]);
    departments.value = departmentList ?? [];
    positions.value = positionList ?? [];
    roles.value = roleList ?? [];
  } finally {
    bootstrapping.value = false;
  }
}

function handleDepartmentSelect(departmentId: null | number) {
  selectedDepartmentId.value = departmentId;
}

function handleAdd() {
  formMode.value = 'add';
  currentEmployee.value = undefined;
  formVisible.value = true;
}

function handleEdit(row: EmployeeRecord) {
  formMode.value = 'edit';
  currentEmployee.value = row;
  formVisible.value = true;
}

async function handleFormSuccess(password?: string) {
  if (!password) {
    formVisible.value = false;
  }
  await tablePanelRef.value?.reload();
}

function handleTotalChange(total: number) {
  employeeTotal.value = total;
}

onMounted(() => {
  void bootstrap().catch((error) => {
    ElMessage.error(error?.message || '员工基础数据加载失败');
  });
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="employee-page">
      <EmployeeOrgTree
        :current-department-id="selectedDepartmentId"
        :departments="departments"
        :loading="bootstrapping"
        :total-count="employeeTotal"
        @select="handleDepartmentSelect"
      />

      <EmployeeTablePanel
        ref="tablePanelRef"
        :departments="departments"
        :positions="positions"
        :roles="roles"
        :selected-department-id="selectedDepartmentId"
        @add="handleAdd"
        @department-change="handleDepartmentSelect"
        @edit="handleEdit"
        @total-change="handleTotalChange"
      />
    </div>

    <EmployeeForm
      v-model:visible="formVisible"
      :departments="departments"
      :employee="currentEmployee"
      :mode="formMode"
      :positions="positions"
      :roles="roles"
      @success="handleFormSuccess"
    />
  </Page>
</template>

<style scoped>
.employee-page {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

@media (width <= 768px) {
  .employee-page {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(260px, 36vh) minmax(0, 1fr);
  }
}
</style>
