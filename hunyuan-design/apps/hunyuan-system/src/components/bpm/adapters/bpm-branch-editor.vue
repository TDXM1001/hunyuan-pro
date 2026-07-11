<script setup lang="ts">
import type { BpmBranchNodeDraft } from './types';

import { ElButton, ElCheckbox, ElFormItem, ElInput, ElTag } from 'element-plus';
import BpmRouteConditionEditor from './bpm-route-condition-editor.vue';

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    fieldOptions: { field: string; label: string; type: string }[];
    node: BpmBranchNodeDraft;
  }>(),
  { disabled: false },
);
const emit = defineEmits<{ change: [] }>();

function addBranch() {
  const index = props.node.branches.length + 1;
  props.node.branches.push({
    branchKey: `branch_${index}`,
    name: `分支${index}`,
    nodes: [],
  });
  emit('change');
}

function removeBranch(index: number) {
  if (props.node.branches.length <= 2) return;
  props.node.branches.splice(index, 1);
  emit('change');
}

function setDefault(index: number, checked: boolean) {
  props.node.branches.forEach((branch, branchIndex) => {
    branch.isDefault = checked && branchIndex === index;
  });
  emit('change');
}
</script>

<template>
  <div class="branch-editor">
    <div class="branch-editor__header">
      <ElTag effect="plain">{{ node.branchType }} split / join</ElTag>
      <ElButton :disabled="disabled" size="small" @click="addBranch">新增分支</ElButton>
    </div>
    <section v-for="(branch, index) in node.branches" :key="branch.branchKey" class="branch-editor__item">
      <div class="branch-editor__identity">
        <ElFormItem label="分支名称"><ElInput v-model="branch.name" :disabled="disabled" @change="emit('change')" /></ElFormItem>
        <ElFormItem label="分支 key"><ElInput v-model="branch.branchKey" :disabled="disabled" @change="emit('change')" /></ElFormItem>
        <ElCheckbox
          v-if="node.branchType !== 'PARALLEL'"
          :disabled="disabled"
          :model-value="branch.isDefault === true"
          @change="(value) => setDefault(index, Boolean(value))"
        >默认分支</ElCheckbox>
        <ElButton :disabled="disabled || node.branches.length <= 2" link type="danger" @click="removeBranch(index)">删除</ElButton>
      </div>
      <BpmRouteConditionEditor
        v-if="node.branchType !== 'PARALLEL' && !branch.isDefault"
        v-model="branch.condition"
        :disabled="disabled"
        :field-options="fieldOptions"
        @update:model-value="emit('change')"
      />
    </section>
  </div>
</template>

<style scoped>
.branch-editor { display: flex; flex-direction: column; gap: 10px; }
.branch-editor__header, .branch-editor__identity { align-items: center; display: flex; gap: 8px; }
.branch-editor__header { justify-content: space-between; }
.branch-editor__item { border-top: 1px solid var(--el-border-color-lighter); padding-top: 10px; }
.branch-editor__identity { flex-wrap: wrap; }
.branch-editor__identity :deep(.el-form-item) { flex: 1 1 150px; margin-bottom: 0; }
</style>
