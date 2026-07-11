<script setup lang="ts">
import type { BpmProcessNodeDraft } from './types';

import { ElButton, ElOption, ElSelect, ElTag } from 'element-plus';

defineOptions({ name: 'BpmProcessTreeEditor' });
const props = withDefaults(
  defineProps<{
    depth?: number;
    disabled?: boolean;
    modelValue: BpmProcessNodeDraft[];
    selectedKey?: string;
  }>(),
  { depth: 0, disabled: false, selectedKey: '' },
);
const emit = defineEmits<{
  'update:modelValue': [value: BpmProcessNodeDraft[]];
  'update:selectedKey': [value: string];
  change: [];
}>();

let sequence = 0;
function nextKey(prefix: string) {
  sequence += 1;
  return `${prefix}_${Date.now().toString(36)}_${sequence}`;
}
function createNode(kind: string): BpmProcessNodeDraft {
  if (['EXCLUSIVE', 'INCLUSIVE', 'PARALLEL'].includes(kind)) {
    const nodeKey = nextKey('route');
    return {
      branches: [
        { branchKey: nextKey('branch'), name: '分支1', nodes: [] },
        { branchKey: nextKey('branch'), isDefault: kind !== 'PARALLEL', name: '默认分支', nodes: [] },
      ],
      branchType: kind as 'EXCLUSIVE' | 'INCLUSIVE' | 'PARALLEL',
      id: nodeKey,
      listeners: [],
      name: `${kind} 分支`,
      nodeKey,
      type: 'branch',
    };
  }
  const nodeKey = nextKey(kind === 'COPY' ? 'copy' : kind === 'HANDLE' ? 'handle' : 'task');
  return {
    approvalMode: 'single',
    candidateResolverType: 'EMPLOYEE',
    id: nodeKey,
    listeners: [],
    name: kind === 'COPY' ? '抄送节点' : kind === 'HANDLE' ? '办理节点' : '审批节点',
    nodeKey,
    type: kind === 'COPY' ? 'copyTask' : kind === 'HANDLE' ? 'handleTask' : 'userTask',
  };
}
function add(kind: string) {
  if (!kind) return;
  const node = createNode(kind);
  emit('update:modelValue', [...props.modelValue, node]);
  emit('update:selectedKey', node.nodeKey);
  emit('change');
}
function move(index: number, offset: number) {
  const target = index + offset;
  if (target < 0 || target >= props.modelValue.length) return;
  const next = [...props.modelValue];
  [next[index], next[target]] = [next[target]!, next[index]!];
  emit('update:modelValue', next); emit('change');
}
function remove(index: number) {
  const next = props.modelValue.filter((_, itemIndex) => itemIndex !== index);
  emit('update:modelValue', next); emit('update:selectedKey', next[0]?.nodeKey || ''); emit('change');
}
function duplicate(index: number) {
  const source = props.modelValue[index];
  if (!source) return;
  const clone = JSON.parse(JSON.stringify(source)) as BpmProcessNodeDraft;
  clone.nodeKey = nextKey('copy'); clone.id = clone.nodeKey; clone.name = `${clone.name}副本`;
  emit('update:modelValue', [...props.modelValue.slice(0, index + 1), clone, ...props.modelValue.slice(index + 1)]);
  emit('update:selectedKey', clone.nodeKey); emit('change');
}
function keyInvalid(key: string) { return !/^[A-Za-z_][A-Za-z0-9_]*$/.test(key); }
</script>

<template>
  <div class="process-tree" :class="`process-tree--depth-${depth}`">
    <div class="process-tree__toolbar">
      <ElSelect :disabled="disabled" placeholder="新增节点" @change="add">
        <ElOption label="审批节点" value="USER" />
        <ElOption label="办理节点" value="HANDLE" />
        <ElOption label="抄送节点" value="COPY" />
        <ElOption v-if="depth < 3" label="排他分支" value="EXCLUSIVE" />
        <ElOption v-if="depth < 3" label="包容分支" value="INCLUSIVE" />
        <ElOption v-if="depth < 3" label="并行分支" value="PARALLEL" />
      </ElSelect>
      <ElTag effect="plain" size="small">深度 {{ depth }} / 3</ElTag>
    </div>
    <div v-for="(node, index) in modelValue" :key="node.id" class="process-tree__node">
      <div
        class="process-tree__row"
        :class="{ 'is-selected': selectedKey === node.nodeKey }"
        @click="emit('update:selectedKey', node.nodeKey)"
      >
        <span class="process-tree__type">{{ node.type === 'branch' ? `${node.branchType} split` : node.type }}</span>
        <strong>{{ node.name }}</strong>
        <code :class="{ 'is-invalid': keyInvalid(node.nodeKey) }">{{ node.nodeKey }}</code>
        <div class="process-tree__commands">
          <ElButton :disabled="disabled || index === 0" link title="上移" @click.stop="move(index, -1)">↑</ElButton>
          <ElButton :disabled="disabled || index === modelValue.length - 1" link title="下移" @click.stop="move(index, 1)">↓</ElButton>
          <ElButton :disabled="disabled" link title="复制" @click.stop="duplicate(index)">⧉</ElButton>
          <ElButton :disabled="disabled" link title="删除" type="danger" @click.stop="remove(index)">×</ElButton>
        </div>
      </div>
      <div v-if="node.type === 'branch'" class="process-tree__branches">
        <section v-for="branch in node.branches" :key="branch.branchKey" class="process-tree__branch">
          <div class="process-tree__branch-title"><span>分支</span><strong>{{ branch.name }}</strong><code>{{ branch.branchKey }}</code></div>
          <BpmProcessTreeEditor
            v-model="branch.nodes"
            :selected-key="selectedKey"
            :depth="depth + 1"
            :disabled="disabled || depth >= 3"
            @change="emit('change')"
            @update:selected-key="emit('update:selectedKey', $event)"
          />
        </section>
        <div class="process-tree__join">join · {{ node.nodeKey }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.process-tree { display: flex; flex-direction: column; gap: 8px; }
.process-tree__toolbar, .process-tree__row, .process-tree__branch-title { align-items: center; display: flex; gap: 8px; }
.process-tree__toolbar { justify-content: space-between; }
.process-tree__toolbar :deep(.el-select) { width: 150px; }
.process-tree__node { border-left: 3px solid var(--el-border-color); padding-left: 8px; }
.process-tree__row { background: var(--el-fill-color-light); border-radius: 6px; min-height: 38px; padding: 4px 8px; }
.process-tree__row.is-selected { background: var(--el-color-primary-light-9); border: 1px solid var(--el-color-primary-light-5); }
.process-tree__type { color: var(--el-text-color-secondary); font-size: 12px; }
.process-tree code { color: var(--el-text-color-secondary); font-size: 11px; }
.process-tree code.is-invalid { color: var(--el-color-danger); }
.process-tree__commands { display: flex; margin-left: auto; }
.process-tree__branches { display: grid; gap: 8px; margin: 8px 0 8px 12px; }
.process-tree__branch { border: 1px solid var(--el-border-color-lighter); border-radius: 6px; padding: 8px; }
.process-tree__join { color: var(--el-text-color-secondary); font-size: 12px; text-align: center; }
</style>
