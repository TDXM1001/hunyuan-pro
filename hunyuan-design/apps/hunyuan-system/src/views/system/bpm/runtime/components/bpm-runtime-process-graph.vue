<script setup lang="ts">
import type { BpmRuntimeGraphRecord } from '#/api/system/bpm/runtime';
import { computed } from 'vue';
import { ElEmpty, ElTag } from 'element-plus';

const props = defineProps<{ graph?: BpmRuntimeGraphRecord | null }>();
const nodes = computed(() => props.graph?.nodes || []);
const stateLabel: Record<string, string> = {
  ACTIVE: '处理中', CANCELLED: '已取消', COMPLETED: '已完成', NOT_ENTERED: '未进入', SKIPPED: '已跳过',
};
const stateType: Record<string, 'danger' | 'info' | 'primary' | 'success' | 'warning'> = {
  ACTIVE: 'primary', CANCELLED: 'warning', COMPLETED: 'success', NOT_ENTERED: 'info', SKIPPED: 'info',
};
</script>

<template>
  <div v-if="nodes.length" class="runtime-process-graph">
    <div v-for="node in nodes" :key="node.definitionNodeId" class="runtime-process-graph__node" :class="`is-${node.state.toLowerCase()}`">
      <div><strong>{{ node.nodeName }}</strong><code>{{ node.nodeKey }}</code></div>
      <small>{{ node.nodeType }}<span v-if="node.branchPath.length"> · {{ node.branchPath.join(' / ') }}</span></small>
      <ElTag :type="stateType[node.state]" effect="plain" size="small">{{ stateLabel[node.state] }}</ElTag>
    </div>
  </div>
  <ElEmpty v-else description="暂无结构化流程路径" />
</template>

<style scoped>
.runtime-process-graph { display: flex; flex-direction: column; gap: 8px; }
.runtime-process-graph__node { align-items: center; border-left: 4px solid var(--el-border-color); display: grid; gap: 8px; grid-template-columns: minmax(150px, 1fr) minmax(120px, 180px) auto; padding: 8px 10px; }
.runtime-process-graph__node.is-active { border-left-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.runtime-process-graph__node.is-completed { border-left-color: var(--el-color-success); }
.runtime-process-graph__node.is-skipped { opacity: .62; }
.runtime-process-graph__node div { display: flex; flex-direction: column; }
.runtime-process-graph code, .runtime-process-graph small { color: var(--el-text-color-secondary); }
</style>
