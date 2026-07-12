<script setup lang="ts">
import type { GraphEdge, GraphNodeType, ProcessDefinitionGraph } from './graph-process-model';
import type { BpmPolicyCatalogRecord } from '#/api/system/bpm';

import { computed, ref, watch } from 'vue';

import { Link2, Paintbrush, Plus, Search } from '@vben/icons';
import { ElButton, ElDivider, ElInput, ElInputNumber, ElOption, ElSelect, ElTag } from 'element-plus';

import {
  autoLayoutGraph,
  diffGraphSemantics,
  simulateGraph,
  updateGraphApprovalPolicy,
  updateGraphBusinessContract,
  updateGraphCandidatePolicy,
  updateGraphEdgeRouteCondition,
  updateGraphStartVisibilityPolicy,
  type GraphDiagnostic,
  type GraphNode,
} from './graph-process-model';

const props = defineProps<{
  baseline?: ProcessDefinitionGraph;
  disabled?: boolean;
  modelValue: ProcessDefinitionGraph;
  policyCatalog?: {
    approvalPolicies: BpmPolicyCatalogRecord[];
    candidatePolicies: BpmPolicyCatalogRecord[];
    startVisibilityPolicies: BpmPolicyCatalogRecord[];
  };
}>();

const emit = defineEmits<{
  'update:modelValue': [graph: ProcessDefinitionGraph];
}>();

const selectedNodeId = ref<string>();
const selectedEdgeId = ref<string>();
const graph = computed(() => props.modelValue);
const selectedNode = computed(() =>
  graph.value.nodes.find((node) => node.nodeId === selectedNodeId.value),
);
const selectedEdge = computed(() =>
  graph.value.edges.find((edge) => edge.edgeId === selectedEdgeId.value),
);
const selectedEdgeSource = computed(() =>
  selectedEdge.value
    ? graph.value.nodes.find((node) => node.nodeId === selectedEdge.value?.sourceNodeId)
    : undefined,
);
const selectedEdgeHasRouteCondition = computed(() =>
  Boolean(selectedEdgeSource.value && isConditionalGateway(selectedEdgeSource.value.type)),
);
const gatewayPairCandidates = computed(() => selectedNode.value
  ? graph.value.nodes.filter((node) => node.nodeId !== selectedNode.value?.nodeId
    && node.scopeId === selectedNode.value?.scopeId
    && node.type === selectedNode.value?.type)
  : []);
const connectionDraft = ref({ sourceNodeId: '', sourcePort: 'default', targetNodeId: '' });
const diagnostics = computed(() => simulateGraph(graph.value).findings);
const semanticDiff = computed(() =>
  props.baseline ? diffGraphSemantics(props.baseline, graph.value) : undefined,
);
const candidatePolicies = computed(() => props.policyCatalog?.candidatePolicies || []);
const approvalPolicies = computed(() => props.policyCatalog?.approvalPolicies || []);
const startVisibilityPolicies = computed(() => props.policyCatalog?.startVisibilityPolicies || []);

const palette: Array<{ label: string; type: GraphNodeType }> = [
  { label: '审批', type: 'APPROVAL' },
  { label: '办理', type: 'HANDLE' },
  { label: '抄送', type: 'COPY' },
  { label: '条件', type: 'CONDITION' },
  { label: '并行', type: 'PARALLEL_GATEWAY' },
  { label: '包容', type: 'INCLUSIVE_GATEWAY' },
];

watch(
  () => graph.value.nodes.map((node) => node.nodeId),
  (nodeIds) => {
    if (!selectedNodeId.value || !nodeIds.includes(selectedNodeId.value)) {
      selectedNodeId.value = nodeIds[0];
    }
    if (selectedEdgeId.value && !graph.value.edges.some((edge) => edge.edgeId === selectedEdgeId.value)) {
      selectedEdgeId.value = undefined;
    }
    if (!connectionDraft.value.sourceNodeId || !nodeIds.includes(connectionDraft.value.sourceNodeId)) {
      connectionDraft.value.sourceNodeId = nodeIds[0] || '';
    }
    if (!connectionDraft.value.targetNodeId || !nodeIds.includes(connectionDraft.value.targetNodeId)) {
      connectionDraft.value.targetNodeId = nodeIds.at(-1) || '';
    }
  },
  { immediate: true },
);

function updateGraph(nextGraph: ProcessDefinitionGraph) {
  emit('update:modelValue', nextGraph);
}

function addNode(type: GraphNodeType) {
  const scopeId = graph.value.rootScopeId;
  const endNode = graph.value.nodes.find((node) => node.type === 'END');
  const predecessorEdge = endNode
    ? graph.value.edges.find((edge) => edge.targetNodeId === endNode.nodeId)
    : undefined;
  const nodeId = `node_${crypto.randomUUID().replaceAll('-', '_')}`;
  const node: GraphNode = {
    name: palette.find((item) => item.type === type)?.label || type,
    nodeId,
    properties: initialNodeProperties(type),
    scopeId,
    type,
  };
  const edges = predecessorEdge
    ? [
         ...graph.value.edges.filter((edge) => edge.edgeId !== predecessorEdge.edgeId),
         {
           ...predecessorEdge,
           edgeId: `edge_${predecessorEdge.sourceNodeId}_${nodeId}`,
           targetNodeId: nodeId,
         },
        {
          edgeId: `edge_${nodeId}_${endNode!.nodeId}`,
           scopeId,
           sourceNodeId: nodeId,
           sourcePort: 'default',
           targetNodeId: endNode!.nodeId,
        },
      ]
    : graph.value.edges;
  updateGraph(autoLayoutGraph({ ...graph.value, edges, nodes: [...graph.value.nodes, node] }));
  selectedNodeId.value = nodeId;
}

function updateSelectedNode(patch: Partial<GraphNode>) {
  if (!selectedNode.value) {
    return;
  }
  updateGraph({
    ...graph.value,
    nodes: graph.value.nodes.map((node) =>
      node.nodeId === selectedNode.value?.nodeId ? { ...node, ...patch } : node,
    ),
  });
}

function updateNodePolicy(
  kind: 'approval' | 'candidate',
  value: string | number | null | undefined,
) {
  if (!selectedNode.value) {
    return;
  }
  const reference = parsePolicySelection(value);
  if (!reference) {
    return;
  }
  updateGraph(kind === 'candidate'
    ? updateGraphCandidatePolicy(graph.value, selectedNode.value.nodeId, reference)
    : updateGraphApprovalPolicy(graph.value, selectedNode.value.nodeId, reference));
}

function updateStartVisibilityPolicy(value: string | number | null | undefined) {
  const reference = parsePolicySelection(value);
  if (!reference) {
    return;
  }
  updateGraph(updateGraphStartVisibilityPolicy(graph.value, reference));
}

function policySelectionValue(value: unknown): string {
  const reference = asRecord(value);
  const policyKey = asText(reference?.policyKey);
  const policyVersion = reference?.policyVersion;
  return policyKey && positiveInteger(policyVersion)
    ? `${encodeURIComponent(policyKey)}:${policyVersion}`
    : '';
}

function catalogPolicySelectionValue(policy: BpmPolicyCatalogRecord): string {
  return policySelectionValue(policy.reference);
}

function parsePolicySelection(value: string | number | null | undefined): Record<string, unknown> | undefined {
  if (typeof value !== 'string') {
    return undefined;
  }
  const separator = value.lastIndexOf(':');
  if (separator <= 0) {
    return undefined;
  }
  const policyKey = decodeURIComponent(value.slice(0, separator));
  const policyVersion = Number(value.slice(separator + 1));
  return policyKey && positiveInteger(policyVersion) ? { policyKey, policyVersion } : undefined;
}

function updateGatewayProperty(field: 'gatewayMode' | 'pairedGatewayId', value: string | number | null | undefined) {
  if (!selectedNode.value) {
    return;
  }
  updateSelectedNode({
    properties: { ...selectedNode.value.properties, [field]: value || undefined },
  });
}

function updateSelectedEdge(patch: Partial<GraphEdge>) {
  if (!selectedEdge.value) {
    return;
  }
  updateGraph({
    ...graph.value,
    edges: graph.value.edges.map((edge) =>
      edge.edgeId === selectedEdge.value?.edgeId ? { ...edge, ...patch } : edge,
    ),
  });
}

function updateRouteCondition(field: string, value: string | number | null | undefined) {
  if (!selectedEdge.value) {
    return;
  }
  updateGraph(updateGraphEdgeRouteCondition(graph.value, selectedEdge.value.edgeId, { [field]: value }));
}

function createConnection() {
  const { sourceNodeId, sourcePort, targetNodeId } = connectionDraft.value;
  if (props.disabled || !sourceNodeId || !targetNodeId || sourceNodeId === targetNodeId) {
    return;
  }
  const edgeId = `edge_${crypto.randomUUID().replaceAll('-', '_')}`;
  updateGraph({
    ...graph.value,
    edges: [
      ...graph.value.edges,
      {
        edgeId,
        properties: {},
        scopeId: graph.value.rootScopeId,
        sourceNodeId,
        sourcePort: sourcePort.trim() || 'default',
        targetNodeId,
      },
    ],
  });
  selectedEdgeId.value = edgeId;
}

function removeSelectedEdge() {
  if (props.disabled || !selectedEdge.value) {
    return;
  }
  updateGraph({
    ...graph.value,
    edges: graph.value.edges.filter((edge) => edge.edgeId !== selectedEdge.value?.edgeId),
  });
  selectedEdgeId.value = undefined;
}

function updateBusinessContract(field: 'contractKey' | 'contractVersion', value: string | number | null | undefined) {
  updateGraph(updateGraphBusinessContract(graph.value, { [field]: value }));
}

function applyAutoLayout() {
  updateGraph(autoLayoutGraph(graph.value));
}

function focusDiagnostic(diagnostic: GraphDiagnostic) {
  if (graph.value.edges.some((edge) => edge.edgeId === diagnostic.elementId)) {
    selectedEdgeId.value = diagnostic.elementId;
    return;
  }
  selectedNodeId.value = diagnostic.elementId;
}

function initialNodeProperties(type: GraphNodeType): Record<string, unknown> {
  if (type === 'APPROVAL') {
    return { approvalPolicy: {}, candidatePolicy: {} };
  }
  if (type === 'HANDLE') {
    return { candidatePolicy: {} };
  }
  if (isGateway(type)) {
    return { gatewayMode: 'SPLIT', pairedGatewayId: '' };
  }
  return {};
}

function isGateway(type: GraphNodeType): boolean {
  return type === 'CONDITION' || type === 'PARALLEL_GATEWAY' || type === 'INCLUSIVE_GATEWAY';
}

function isConditionalGateway(type: GraphNodeType): boolean {
  return type === 'CONDITION' || type === 'INCLUSIVE_GATEWAY';
}

function asRecord(value: unknown): Record<string, unknown> | undefined {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : undefined;
}

function asText(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim().length > 0 ? value : undefined;
}

function positiveInteger(value: unknown): value is number {
  return typeof value === 'number' && Number.isInteger(value) && value > 0;
}
</script>

<template>
  <div class="graph-designer">
    <aside class="graph-designer__palette">
      <div class="graph-designer__section-title">节点</div>
      <ElButton
        v-for="item in palette"
        :key="item.type"
        class="graph-designer__palette-item"
        :disabled="disabled"
        :icon="Plus"
        plain
        @click="addNode(item.type)"
      >
        {{ item.label }}
      </ElButton>
      <ElDivider />
      <ElButton :disabled="disabled" :icon="Paintbrush" plain @click="applyAutoLayout">
        自动布局
      </ElButton>
      <ElButton :icon="Search" plain @click="selectedNodeId = diagnostics[0]?.elementId">
        模拟诊断
      </ElButton>
    </aside>

    <main class="graph-designer__canvas" aria-label="流程图画布">
      <svg class="graph-designer__wires" aria-label="流程连线" role="group">
        <line
          v-for="edge in graph.edges"
          :key="edge.edgeId"
          :class="{ 'graph-designer__wire--selected': selectedEdgeId === edge.edgeId }"
          :x1="(graph.nodes.find((node) => node.nodeId === edge.sourceNodeId)?.layout?.x || 0) + 144"
          :x2="graph.nodes.find((node) => node.nodeId === edge.targetNodeId)?.layout?.x || 0"
          :y1="(graph.nodes.find((node) => node.nodeId === edge.sourceNodeId)?.layout?.y || 0) + 36"
          :y2="(graph.nodes.find((node) => node.nodeId === edge.targetNodeId)?.layout?.y || 0) + 36"
          role="button"
          tabindex="0"
          @click="selectedEdgeId = edge.edgeId"
          @keydown.enter.prevent="selectedEdgeId = edge.edgeId"
        />
      </svg>
      <button
        v-for="node in graph.nodes"
        :key="node.nodeId"
        class="graph-designer__node"
        :class="{ 'graph-designer__node--selected': selectedNodeId === node.nodeId }"
        :style="{ left: `${node.layout?.x || 64}px`, top: `${node.layout?.y || 80}px` }"
        type="button"
        @click="selectedNodeId = node.nodeId"
      >
        <span>{{ node.name }}</span>
        <small>{{ node.type }}</small>
      </button>
    </main>

    <aside class="graph-designer__properties">
      <div class="graph-designer__section-title">流程契约</div>
      <label class="graph-designer__field">
        <span>业务契约</span>
        <ElInput
          :disabled="disabled"
          :model-value="String(asRecord(graph.policies.businessContract)?.contractKey || '')"
          placeholder="contractKey"
          @update:model-value="updateBusinessContract('contractKey', $event)"
        />
      </label>
      <label class="graph-designer__field">
        <span>契约版本</span>
        <ElInputNumber
          :disabled="disabled"
          :min="1"
          :model-value="Number(asRecord(graph.policies.businessContract)?.contractVersion || 1)"
          @update:model-value="updateBusinessContract('contractVersion', $event)"
        />
      </label>
      <label class="graph-designer__field">
        <span>发起可见策略</span>
        <ElSelect
          :disabled="disabled"
          :model-value="policySelectionValue(graph.policies.startVisibilityPolicy)"
          placeholder="选择已启用策略版本"
          @update:model-value="updateStartVisibilityPolicy($event)"
        >
          <ElOption
            v-for="policy in startVisibilityPolicies"
            :key="catalogPolicySelectionValue(policy)"
            :label="`${policy.reference.policyKey} v${policy.reference.policyVersion}`"
            :value="catalogPolicySelectionValue(policy)"
          />
        </ElSelect>
      </label>

      <template v-if="selectedNode">
        <ElDivider />
        <div class="graph-designer__section-title">节点属性</div>
        <label class="graph-designer__field">
          <span>显示名称</span>
          <ElInput :disabled="disabled" :model-value="selectedNode.name" @update:model-value="updateSelectedNode({ name: $event })" />
        </label>
        <label class="graph-designer__field">
          <span>节点类型</span>
          <ElSelect :disabled="disabled" :model-value="selectedNode.type" @update:model-value="updateSelectedNode({ type: $event })">
            <ElOption v-for="item in palette" :key="item.type" :label="item.label" :value="item.type" />
            <ElOption label="开始" value="START" />
            <ElOption label="结束" value="END" />
          </ElSelect>
        </label>
        <template v-if="selectedNode.type === 'APPROVAL' || selectedNode.type === 'HANDLE'">
          <label class="graph-designer__field">
            <span>候选策略</span>
            <ElSelect
              :disabled="disabled"
              :model-value="policySelectionValue(selectedNode.properties?.candidatePolicy)"
              placeholder="选择已启用策略版本"
              @update:model-value="updateNodePolicy('candidate', $event)"
            >
              <ElOption
                v-for="policy in candidatePolicies"
                :key="catalogPolicySelectionValue(policy)"
                :label="`${policy.reference.policyKey} v${policy.reference.policyVersion}`"
                :value="catalogPolicySelectionValue(policy)"
              />
            </ElSelect>
          </label>
          <label v-if="selectedNode.type === 'APPROVAL'" class="graph-designer__field">
            <span>审批策略</span>
            <ElSelect
              :disabled="disabled"
              :model-value="policySelectionValue(selectedNode.properties?.approvalPolicy)"
              placeholder="选择已启用策略版本"
              @update:model-value="updateNodePolicy('approval', $event)"
            >
              <ElOption
                v-for="policy in approvalPolicies"
                :key="catalogPolicySelectionValue(policy)"
                :label="`${policy.reference.policyKey} v${policy.reference.policyVersion}`"
                :value="catalogPolicySelectionValue(policy)"
              />
            </ElSelect>
          </label>
        </template>
        <template v-if="isGateway(selectedNode.type)">
          <label class="graph-designer__field">
            <span>网关模式</span>
            <ElSelect
              :disabled="disabled"
              :model-value="String(selectedNode.properties?.gatewayMode || '')"
              @update:model-value="updateGatewayProperty('gatewayMode', $event)"
            >
              <ElOption label="分叉" value="SPLIT" />
              <ElOption label="汇合" value="JOIN" />
            </ElSelect>
          </label>
          <label class="graph-designer__field">
            <span>配对网关</span>
            <ElSelect
              clearable
              :disabled="disabled"
              :model-value="String(selectedNode.properties?.pairedGatewayId || '')"
              @update:model-value="updateGatewayProperty('pairedGatewayId', $event)"
            >
              <ElOption
                v-for="candidate in gatewayPairCandidates"
                :key="candidate.nodeId"
                :label="candidate.name"
                :value="candidate.nodeId"
              />
            </ElSelect>
          </label>
        </template>
      </template>

      <template v-if="selectedEdge">
        <ElDivider />
        <div class="graph-designer__section-title">连线属性</div>
        <label class="graph-designer__field">
          <span>分支端口</span>
          <ElInput
            :disabled="disabled"
            :model-value="selectedEdge.sourcePort || ''"
            placeholder="default"
            @update:model-value="updateSelectedEdge({ sourcePort: $event })"
          />
        </label>
        <template v-if="selectedEdgeHasRouteCondition && selectedEdge.sourcePort !== 'default'">
          <label class="graph-designer__field">
            <span>条件来源</span>
            <ElSelect
              :disabled="disabled"
              :model-value="String(asRecord(selectedEdge.properties?.routeCondition)?.sourceType || 'FORM_FIELD')"
              @update:model-value="updateRouteCondition('sourceType', $event)"
            >
              <ElOption label="表单字段" value="FORM_FIELD" />
              <ElOption label="实例上下文" value="INSTANCE_CONTEXT" />
              <ElOption label="登记表达式" value="REGISTERED_EXPRESSION" />
            </ElSelect>
          </label>
          <label class="graph-designer__field">
            <span>字段或表达式</span>
            <ElInput
              :disabled="disabled"
              :model-value="String(asRecord(selectedEdge.properties?.routeCondition)?.fieldKey || asRecord(selectedEdge.properties?.routeCondition)?.expressionKey || '')"
              @update:model-value="updateRouteCondition('fieldKey', $event)"
            />
          </label>
          <label class="graph-designer__field">
            <span>值类型</span>
            <ElSelect
              :disabled="disabled"
              :model-value="String(asRecord(selectedEdge.properties?.routeCondition)?.valueType || 'NUMBER')"
              @update:model-value="updateRouteCondition('valueType', $event)"
            >
              <ElOption label="数字" value="NUMBER" />
              <ElOption label="文本" value="STRING" />
              <ElOption label="布尔" value="BOOLEAN" />
            </ElSelect>
          </label>
          <label class="graph-designer__field">
            <span>操作符</span>
            <ElSelect
              :disabled="disabled"
              :model-value="String(asRecord(selectedEdge.properties?.routeCondition)?.operator || 'EQ')"
              @update:model-value="updateRouteCondition('operator', $event)"
            >
              <ElOption label="等于" value="EQ" />
              <ElOption label="大于" value="GT" />
              <ElOption label="大于等于" value="GTE" />
              <ElOption label="小于" value="LT" />
              <ElOption label="小于等于" value="LTE" />
            </ElSelect>
          </label>
          <label class="graph-designer__field">
            <span>比较值</span>
            <ElInput
              :disabled="disabled"
              :model-value="String(asRecord(selectedEdge.properties?.routeCondition)?.compareValue || '')"
              @update:model-value="updateRouteCondition('compareValue', $event)"
            />
          </label>
        </template>
      </template>

      <ElDivider />
      <div class="graph-designer__section-title">连接</div>
      <label class="graph-designer__field">
        <span>起点</span>
        <ElSelect v-model="connectionDraft.sourceNodeId" :disabled="disabled">
          <ElOption v-for="node in graph.nodes" :key="node.nodeId" :label="node.name" :value="node.nodeId" />
        </ElSelect>
      </label>
      <label class="graph-designer__field">
        <span>终点</span>
        <ElSelect v-model="connectionDraft.targetNodeId" :disabled="disabled">
          <ElOption v-for="node in graph.nodes" :key="node.nodeId" :label="node.name" :value="node.nodeId" />
        </ElSelect>
      </label>
      <label class="graph-designer__field">
        <span>端口</span>
        <ElInput v-model="connectionDraft.sourcePort" :disabled="disabled" placeholder="default" />
      </label>
      <div class="graph-designer__connection-actions">
        <ElButton :disabled="disabled" :icon="Plus" plain @click="createConnection">新增连线</ElButton>
        <ElButton :disabled="disabled || !selectedEdge" link type="danger" @click="removeSelectedEdge">删除</ElButton>
      </div>

      <ElDivider />
      <div class="graph-designer__section-title">模拟</div>
      <button
        v-for="finding in diagnostics"
        :key="`${finding.elementId}-${finding.path}`"
        class="graph-designer__finding"
        type="button"
        @click="focusDiagnostic(finding)"
      >
        <ElTag type="danger">阻断</ElTag>
        <span>{{ finding.message }}</span>
      </button>
      <ElTag v-if="diagnostics.length === 0" type="success">模拟通过</ElTag>

      <template v-if="semanticDiff">
        <ElDivider />
        <div class="graph-designer__section-title">语义 Diff</div>
        <ElTag :type="semanticDiff.hasSemanticChanges ? 'warning' : 'success'">
          {{ semanticDiff.hasSemanticChanges ? `${semanticDiff.changedElements.length} 项语义变更` : '仅布局或无变更' }}
        </ElTag>
        <div v-for="change in semanticDiff.changedElements" :key="`${change.kind}-${change.elementId}`" class="graph-designer__diff-item">
          <Link2 :size="14" /> {{ change.summary }}：{{ change.elementId }}
        </div>
      </template>
    </aside>
  </div>
</template>

<style scoped>
.graph-designer { display: grid; grid-template-columns: 148px minmax(520px, 1fr) 280px; min-height: 620px; border: 1px solid var(--el-border-color); background: var(--el-bg-color); }
.graph-designer__palette, .graph-designer__properties { display: flex; flex-direction: column; gap: 8px; padding: 12px; overflow: auto; background: var(--el-fill-color-lighter); }
.graph-designer__palette { border-right: 1px solid var(--el-border-color); }
.graph-designer__properties { border-left: 1px solid var(--el-border-color); }
.graph-designer__palette-item { justify-content: flex-start; margin-left: 0; }
.graph-designer__canvas { position: relative; min-width: 0; overflow: auto; background-image: linear-gradient(var(--el-border-color-lighter) 1px, transparent 1px), linear-gradient(90deg, var(--el-border-color-lighter) 1px, transparent 1px); background-size: 20px 20px; }
.graph-designer__wires { position: absolute; inset: 0; width: 100%; height: 100%; min-width: 720px; }
.graph-designer__wires line { cursor: pointer; pointer-events: stroke; stroke: var(--el-color-primary-light-5); stroke-width: 8; stroke-opacity: 0; }
.graph-designer__wires line::after { content: ''; }
.graph-designer__wires line { stroke-dasharray: 6 4; }
.graph-designer__wires line:not(.graph-designer__wire--selected) { stroke: var(--el-color-primary-light-5); stroke-opacity: 1; stroke-width: 2; }
.graph-designer__wire--selected { stroke: var(--el-color-primary); stroke-opacity: 1; stroke-width: 4; }
.graph-designer__node { position: absolute; z-index: 1; width: 144px; min-height: 72px; padding: 10px; border: 1px solid var(--el-border-color); border-radius: 6px; background: var(--el-bg-color); color: var(--el-text-color-primary); text-align: left; box-shadow: 0 1px 2px rgb(0 0 0 / 8%); }
.graph-designer__node--selected { border-color: var(--el-color-primary); box-shadow: 0 0 0 2px var(--el-color-primary-light-8); }
.graph-designer__node small { display: block; margin-top: 6px; color: var(--el-text-color-secondary); font-size: 11px; }
.graph-designer__section-title { color: var(--el-text-color-primary); font-size: 13px; font-weight: 600; }
.graph-designer__field { display: grid; gap: 4px; color: var(--el-text-color-secondary); font-size: 12px; }
.graph-designer__connection-actions { display: flex; align-items: center; gap: 8px; }
.graph-designer__finding { display: flex; gap: 6px; align-items: flex-start; border: 0; background: transparent; color: var(--el-text-color-regular); cursor: pointer; text-align: left; }
.graph-designer__diff-item { display: flex; gap: 4px; align-items: center; color: var(--el-text-color-secondary); font-size: 12px; }
@media (max-width: 1100px) { .graph-designer { grid-template-columns: 132px minmax(460px, 1fr); } .graph-designer__properties { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); border-top: 1px solid var(--el-border-color); border-left: 0; } }
</style>
