<script setup lang="ts">
import type { OrgTreeEmits, OrgTreeNode, OrgTreeProps } from './types';

import { computed, ref, watch } from 'vue';

import { ElTree } from 'element-plus';

defineOptions({ name: 'ArtOrgTree' });

const props = withDefaults(defineProps<OrgTreeProps>(), {
  showCount: true,
  defaultExpandAll: true,
  highlightCurrent: true,
  nodeKey: 'id',
  currentNodeKey: undefined,
  emptyText: '暂无数据',
  filterNodeMethod: undefined,
});

const emit = defineEmits<OrgTreeEmits>();

const treeRef = ref<InstanceType<typeof ElTree>>();
const filterText = ref('');

const treeProps = {
  children: 'children',
  label: 'label',
  disabled: 'disabled',
};

const internalCurrentKey = ref<null | number | string>(
  props.currentNodeKey ?? null,
);
const elementCurrentKey = computed(() => internalCurrentKey.value ?? undefined);

watch(
  () => props.currentNodeKey,
  (newKey) => {
    internalCurrentKey.value = newKey ?? null;
    if (treeRef.value) {
      treeRef.value.setCurrentKey(newKey ?? undefined);
    }
  },
);

watch(filterText, (val) => {
  treeRef.value?.filter(val);
});

function handleNodeClick(data: OrgTreeNode, node: any) {
  internalCurrentKey.value = data[props.nodeKey];
  emit('update:currentNodeKey', internalCurrentKey.value);
  emit('node-click', data, node);
}

function handleCurrentChange(data: null | OrgTreeNode, node: null | any) {
  if (data) {
    internalCurrentKey.value = data[props.nodeKey];
    emit('update:currentNodeKey', internalCurrentKey.value);
  } else {
    internalCurrentKey.value = null;
    emit('update:currentNodeKey', null);
  }
  emit('current-change', data, node);
}

function filterNode(value: string, data: any, node: any) {
  if (props.filterNodeMethod) {
    return props.filterNodeMethod(value, data, node);
  }
  if (!value) return true;
  return data.label.includes(value);
}

const hasFilterSlot = computed(() => !!slots.filter);

const slots = defineSlots<{
  default?: (scope: { node: any; data: OrgTreeNode }) => any;
  empty?: () => any;
  filter?: () => any;
}>();

defineExpose({
  filter: (value: string) => {
    filterText.value = value;
  },
  clearFilter: () => {
    filterText.value = '';
  },
  getNode: (key: number | string) => treeRef.value?.getNode(key),
  setCurrentKey: (key: null | number | string) => {
    internalCurrentKey.value = key;
    treeRef.value?.setCurrentKey(key ?? undefined);
    emit('update:currentNodeKey', key);
  },
});
</script>

<template>
  <div class="art-org-tree">
    <div v-if="hasFilterSlot || $slots.filter" class="art-org-tree__filter">
      <slot name="filter">
        <ElInput
          v-model="filterText"
          clearable
          placeholder="搜索部门"
          prefix-icon="Search"
        />
      </slot>
    </div>

    <div class="art-org-tree__body">
      <ElTree
        ref="treeRef"
        :current-node-key="elementCurrentKey"
        :data="data"
        :default-expand-all="defaultExpandAll"
        :empty-text="emptyText"
        :filter-node-method="filterNode"
        :highlight-current="highlightCurrent"
        :node-key="nodeKey"
        :props="treeProps"
        @current-change="handleCurrentChange"
        @node-click="handleNodeClick"
      >
        <template #default="{ node, data: nodeData }">
          <slot :data="nodeData" :node="node">
            <span class="art-org-tree__node">
              <span class="art-org-tree__node-label">{{ nodeData.label }}</span>
              <span
                v-if="showCount && nodeData.count !== undefined"
                class="art-org-tree__node-count"
              >
                {{ nodeData.count }}
              </span>
            </span>
          </slot>
        </template>

        <template v-if="$slots.empty" #empty>
          <slot name="empty" />
        </template>
      </ElTree>
    </div>
  </div>
</template>

<style scoped>
.art-org-tree {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.art-org-tree__filter {
  flex-shrink: 0;
  margin-bottom: 12px;
}

.art-org-tree__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.art-org-tree__node {
  display: flex;
  align-items: center;
  flex: 1;
  justify-content: space-between;
  overflow: hidden;
  padding-right: 8px;
}

.art-org-tree__node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.art-org-tree__node-count {
  flex-shrink: 0;
  background: var(--el-color-info-light-9);
  border-radius: 10px;
  color: var(--el-color-info);
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  margin-left: 8px;
  padding: 3px 8px;
}

.art-org-tree :deep(.el-tree-node__content) {
  height: 36px;
}

.art-org-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.art-org-tree
  :deep(.el-tree-node.is-current > .el-tree-node__content .art-org-tree__node-count) {
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
}
</style>
