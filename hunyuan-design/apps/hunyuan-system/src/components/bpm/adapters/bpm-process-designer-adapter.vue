<script setup lang="ts">
import type {
  BpmProcessDesignerExpose,
  BpmProcessDesignerSnapshot,
  BpmProcessNodeDraft,
} from './types';

import BpmnModeler from 'bpmn-js/lib/Modeler';
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

import {
  ElButton,
  ElCard,
  ElFormItem,
  ElInput,
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

import {
  buildReadonlyBpmnXml,
  parseSimpleModelDraft,
} from './simple-model-bridge';

defineOptions({ name: 'BpmProcessDesignerAdapter' });

const emit = defineEmits<{
  change: [snapshot: BpmProcessDesignerSnapshot];
  ready: [];
}>();

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    initialSnapshot?: Partial<BpmProcessDesignerSnapshot>;
    modelKey?: string;
    modelName?: string;
    readonly?: boolean;
  }>(),
  {
    disabled: false,
    initialSnapshot: () => ({}),
    modelKey: '',
    modelName: '',
    readonly: false,
  },
);

const canvasRef = ref<HTMLDivElement>();
const dirty = ref(false);
const modeler = ref<any>();
const nodes = ref<BpmProcessNodeDraft[]>([]);
const selectedNodeId = ref('');

const selectedNode = computed(() =>
  nodes.value.find((item) => item.nodeKey === selectedNodeId.value),
);

function buildEmptyNode(index: number): BpmProcessNodeDraft {
  const nodeKey = `task_${index}`;

  return {
    approvalMode: 'single',
    candidateResolverType: 'EMPLOYEE',
    id: nodeKey,
    listeners: [],
    name: `审批节点${index}`,
    nodeKey,
    type: 'userTask',
  };
}

async function ensureModeler() {
  if (modeler.value || !canvasRef.value) {
    return;
  }

  modeler.value = new BpmnModeler({
    container: canvasRef.value,
  });
}

async function renderCanvas() {
  await ensureModeler();

  const xml = buildReadonlyBpmnXml(
    props.modelKey || 'process_model',
    props.modelName || '流程模型',
    nodes.value,
  );

  await nextTick();
  await modeler.value?.importXML?.(xml);
  await modeler.value?.get?.('canvas')?.zoom?.('fit-viewport');
}

async function load(snapshot: Partial<BpmProcessDesignerSnapshot>) {
  nodes.value = snapshot.nodes?.length ? snapshot.nodes : [];
  selectedNodeId.value = nodes.value[0]?.nodeKey || '';
  await renderCanvas();
  dirty.value = false;
}

function getSnapshot(): BpmProcessDesignerSnapshot {
  return {
    bpmnXml: buildReadonlyBpmnXml(
      props.modelKey || 'process_model',
      props.modelName || '流程模型',
      nodes.value,
    ),
    nodes: nodes.value,
  };
}

async function validate() {
  const hasNodes = nodes.value.length > 0;

  return {
    message: hasNodes ? undefined : '请至少保留一个审批节点',
    ok: hasNodes,
  };
}

function resetDirty() {
  dirty.value = false;
}

function isDirty() {
  return dirty.value;
}

async function handleStateChange() {
  dirty.value = true;
  emit('change', getSnapshot());
  await renderCanvas();
}

async function addNode() {
  const nextIndex = nodes.value.length + 1;
  const newNode = buildEmptyNode(nextIndex);

  nodes.value = [...nodes.value, newNode];
  selectedNodeId.value = newNode.nodeKey;
  await handleStateChange();
}

async function removeNode(nodeKey: string) {
  nodes.value = nodes.value.filter((item) => item.nodeKey !== nodeKey);
  selectedNodeId.value = nodes.value[0]?.nodeKey || '';
  await handleStateChange();
}

async function handleListenersChange(value: string) {
  if (!selectedNode.value) {
    return;
  }

  selectedNode.value.listeners = JSON.parse(value || '[]');
  await handleStateChange();
}

defineExpose<BpmProcessDesignerExpose>({
  getSnapshot,
  isDirty,
  load,
  resetDirty,
  validate,
});

onMounted(async () => {
  await ensureModeler();
  await load({
    nodes: props.initialSnapshot?.nodes || [],
  });
  if (!nodes.value.length) {
    nodes.value = parseSimpleModelDraft('{"nodes":[]}');
    await renderCanvas();
  }
  emit('ready');
});

onBeforeUnmount(() => {
  modeler.value?.destroy?.();
  modeler.value = undefined;
});
</script>

<template>
  <div class="bpm-process-designer-adapter">
    <div class="bpm-process-designer-adapter__toolbar">
      <ElSpace>
        <ElButton
          :disabled="disabled || readonly"
          type="primary"
          @click="addNode"
        >
          新增审批节点
        </ElButton>
        <ElTag effect="plain" type="info">
          当前节点数：{{ nodes.length }}
        </ElTag>
      </ElSpace>
    </div>

    <div class="bpm-process-designer-adapter__body">
      <div ref="canvasRef" class="bpm-process-designer-adapter__canvas"></div>

      <ElCard class="bpm-process-designer-adapter__panel" shadow="never">
        <template #header>
          <div class="bpm-process-designer-adapter__panel-header">
            <span>节点属性</span>
            <ElSelect
              v-model="selectedNodeId"
              class="bpm-process-designer-adapter__panel-select"
              placeholder="请选择节点"
            >
              <ElOption
                v-for="item in nodes"
                :key="item.nodeKey"
                :label="item.name"
                :value="item.nodeKey"
              />
            </ElSelect>
          </div>
        </template>

        <template v-if="selectedNode">
          <ElFormItem label="节点名称">
            <ElInput
              v-model="selectedNode.name"
              :disabled="disabled || readonly"
              @change="handleStateChange"
            />
          </ElFormItem>
          <ElFormItem label="候选人解析类型">
            <ElSelect
              v-model="selectedNode.candidateResolverType"
              :disabled="disabled || readonly"
              @change="handleStateChange"
            >
              <ElOption label="指定员工" value="EMPLOYEE" />
              <ElOption label="部门负责人" value="DEPARTMENT_MANAGER" />
              <ElOption label="发起人本人" value="START_EMPLOYEE" />
              <ElOption
                label="发起人部门主管"
                value="START_DEPARTMENT_MANAGER"
              />
              <ElOption label="角色" value="ROLE" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="审批模式">
            <ElSelect
              v-model="selectedNode.approvalMode"
              :disabled="disabled || readonly"
              @change="handleStateChange"
            >
              <ElOption label="单人审批" value="single" />
              <ElOption label="单人审批（严格）" value="singleOnly" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="监听器 JSON">
            <ElInput
              :disabled="disabled || readonly"
              :model-value="JSON.stringify(selectedNode.listeners || [], null, 2)"
              :rows="6"
              type="textarea"
              @change="handleListenersChange"
            />
          </ElFormItem>
          <ElButton
            :disabled="disabled || readonly"
            type="danger"
            @click="removeNode(selectedNodeId)"
          >
            删除当前节点
          </ElButton>
        </template>

        <div v-else class="bpm-process-designer-adapter__empty">
          请先新增一个审批节点
        </div>
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
.bpm-process-designer-adapter {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 560px;
}

.bpm-process-designer-adapter__toolbar {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.bpm-process-designer-adapter__body {
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(0, 1fr) 320px;
  min-height: 520px;
}

.bpm-process-designer-adapter__canvas {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  min-height: 520px;
  overflow: hidden;
}

.bpm-process-designer-adapter__panel {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.bpm-process-designer-adapter__panel :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bpm-process-designer-adapter__panel-header {
  align-items: center;
  display: flex;
  gap: 8px;
  justify-content: space-between;
}

.bpm-process-designer-adapter__panel-select {
  width: 168px;
}

.bpm-process-designer-adapter__empty {
  align-items: center;
  color: var(--el-text-color-secondary);
  display: flex;
  justify-content: center;
  min-height: 240px;
}

@media (width <= 1200px) {
  .bpm-process-designer-adapter__body {
    grid-template-columns: 1fr;
  }
}
</style>
