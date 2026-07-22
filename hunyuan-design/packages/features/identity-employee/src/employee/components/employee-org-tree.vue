<script setup lang="ts">
import type { OrgTreeNode } from '@vben/art-hooks/tree';

import type { DepartmentOption } from '../contract';

import { computed } from 'vue';

import { ArtOrgTree } from '@vben/art-hooks/tree';

import { Folder, OfficeBuilding, User } from '@element-plus/icons-vue';
import { ElCard, ElIcon, ElSkeleton } from 'element-plus';

defineOptions({ name: 'EmployeeOrgTree' });

interface EmployeeOrgTreeProps {
  departments: DepartmentOption[];
  currentDepartmentId: null | number;
  totalCount: number;
  loading?: boolean;
}

const props = withDefaults(defineProps<EmployeeOrgTreeProps>(), {
  loading: false,
});

const emit = defineEmits<{
  select: [departmentId: null | number];
}>();

const currentNodeKey = computed(() => props.currentDepartmentId ?? 0);

function sortDepartments(a: DepartmentOption, b: DepartmentOption) {
  return (b.sort ?? 0) - (a.sort ?? 0);
}

// 后端顶级部门 parent_id 使用 0，历史接口也可能返回 null；这里统一识别为根节点。
const orgTreeData = computed<OrgTreeNode[]>(() => {
  const departmentIds = new Set(
    props.departments.map((department) => department.departmentId),
  );
  const rootDepartments = props.departments
    .filter((department) => {
      const parentId = department.parentId ?? 0;
      return parentId === 0 || !departmentIds.has(parentId);
    })
    .sort(sortDepartments);

  const buildChildren = (
    parentId: number,
    visited = new Set<number>(),
  ): OrgTreeNode[] => {
    if (visited.has(parentId)) {
      return [];
    }

    const nextVisited = new Set(visited);
    nextVisited.add(parentId);

    return props.departments
      .filter((department) => department.parentId === parentId)
      .sort(sortDepartments)
      .map((department) => ({
        id: department.departmentId,
        label: department.departmentName,
        parentId: department.parentId,
        children: buildChildren(department.departmentId, nextVisited),
      }));
  };

  return [
    {
      id: 0,
      label: '全公司',
      children: rootDepartments.map((department) => ({
        id: department.departmentId,
        label: department.departmentName,
        parentId: department.parentId,
        children: buildChildren(department.departmentId),
      })),
    },
  ];
});

function handleNodeClick(node: OrgTreeNode) {
  emit('select', node.id === 0 ? null : Number(node.id));
}
</script>

<template>
  <ElCard class="employee-org-tree" shadow="never">
    <template #header>
      <div class="employee-org-tree__header">
        <div>
          <div class="employee-org-tree__title">机构树</div>
        </div>
      </div>
    </template>

    <ElSkeleton v-if="loading" :rows="8" animated />
    <ArtOrgTree
      v-else
      :current-node-key="currentNodeKey"
      :data="orgTreeData"
      :default-expand-all="true"
      empty-text="暂无机构"
      node-key="id"
      :show-count="false"
      @node-click="handleNodeClick"
    >
      <template #default="{ data }">
        <span class="employee-org-tree__node">
          <ElIcon class="employee-org-tree__node-icon">
            <OfficeBuilding v-if="data.id === 0" />
            <Folder v-else-if="data.children?.length" />
            <User v-else />
          </ElIcon>
          <span class="employee-org-tree__node-label">{{ data.label }}</span>
        </span>
      </template>
    </ArtOrgTree>
  </ElCard>
</template>

<style scoped>
.employee-org-tree {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  min-height: 0;
  overflow: hidden;
}

.employee-org-tree :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: calc(100% - 55px);
  min-height: 0;
  overflow: hidden;
  padding: 12px 12px;
}

.employee-org-tree :deep(.el-card__header) {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 16px 24px;
}

.employee-org-tree__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.employee-org-tree__title {
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
}

.employee-org-tree :deep(.el-tree-node__content) {
  border-radius: 4px;
  color: var(--el-text-color-regular);
  height: 36px;
  padding-right: 10px;
}

.employee-org-tree :deep(.el-tree-node__expand-icon) {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.employee-org-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 600;
}

.employee-org-tree :deep(.el-tree-node__content:hover) {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.employee-org-tree__node {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  min-width: 0;
  font-size: 14px;
  line-height: 20px;
}

.employee-org-tree__node-icon {
  color: var(--el-color-primary);
  flex-shrink: 0;
  font-size: 16px;
}

.employee-org-tree__node-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (width <= 768px) {
  .employee-org-tree :deep(.el-card__header) {
    padding: 12px 16px;
  }

  .employee-org-tree :deep(.el-card__body) {
    height: calc(100% - 47px);
  }
}
</style>
