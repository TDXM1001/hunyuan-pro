<script setup lang="ts">
import type { ProcessDefinitionGraph } from '#/components/bpm/graph/graph-process-model';
import type { BpmGraphDefinitionDetailRecord } from '#/api/system/bpm';

import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { ArrowLeft, Check, CircleCheckBig, Plus, Search } from '@vben/icons';
import {
  ElButton,
  ElDialog,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag,
} from 'element-plus';

import {
  createBpmGraphDraft,
  deactivateBpmGraphDefinition,
  getBpmGraphDefinitionDetail,
  getBpmGraphDraft,
  getLatestBpmGraphDefinitionDetail,
  publishBpmGraphDefinition,
  restoreBpmGraphDraft,
  saveBpmGraphDraft,
} from '#/api/system/bpm';
import GraphProcessDesigner from '#/components/bpm/graph/graph-process-designer.vue';
import {
  cloneGraphDocument,
  semanticFingerprint,
  simulateGraph,
} from '#/components/bpm/graph/graph-process-model';

defineOptions({ name: 'SystemBpmModelEditor' });

interface DraftMeta {
  categoryId?: number;
  processKey: string;
  processName: string;
}

const route = useRoute();
const router = useRouter();
const busy = ref(false);
const draftId = ref<number>();
const revision = ref<number>();
const graph = ref(createEmptyGraph());
const baseline = ref<ProcessDefinitionGraph>();
const draftMeta = reactive<DraftMeta>({ processKey: '', processName: '' });
const definitionDetailVisible = ref(false);
const publishedDefinition = ref<BpmGraphDefinitionDetailRecord>();

const hasDraft = computed(() => Boolean(draftId.value && revision.value));
const simulation = computed(() => simulateGraph(graph.value));
const dirty = computed(() => {
  const savedGraph = baseline.value;
  return savedGraph
    ? semanticFingerprint(savedGraph) !== semanticFingerprint(graph.value)
    : false;
});

function readDraftId() {
  const value = Array.isArray(route.query.draftId) ? route.query.draftId[0] : route.query.draftId;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined;
}

function createEmptyGraph(): ProcessDefinitionGraph {
  return {
    edges: [
      {
        edgeId: 'edge_start_end',
        scopeId: 'scope_root',
        sourceNodeId: 'node_start',
        targetNodeId: 'node_end',
      },
    ],
    nodes: [
      { layout: { x: 64, y: 80 }, name: '开始', nodeId: 'node_start', scopeId: 'scope_root', type: 'START' },
      { layout: { x: 304, y: 80 }, name: '结束', nodeId: 'node_end', scopeId: 'scope_root', type: 'END' },
    ],
    policies: {},
    rootScopeId: 'scope_root',
    schemaVersion: 1,
    scopes: [{ name: '主流程', scopeId: 'scope_root' }],
  };
}

async function loadDraft(targetDraftId: number) {
  busy.value = true;
  try {
    const [record, latestDefinition] = await Promise.all([
      getBpmGraphDraft(targetDraftId),
      getLatestBpmGraphDefinitionDetail(targetDraftId),
    ]);
    draftId.value = record.draftId;
    revision.value = record.revision;
    graph.value = restoreBpmGraphDraft(record);
    baseline.value = cloneGraphDocument(graph.value);
    publishedDefinition.value = latestDefinition;
  } finally {
    busy.value = false;
  }
}

async function createDraft() {
  if (!draftMeta.processKey.trim() || !draftMeta.processName.trim()) {
    ElMessage.warning('请输入流程编码和流程名称');
    return;
  }
  busy.value = true;
  try {
    const record = await createBpmGraphDraft({ ...draftMeta, graph: graph.value });
    draftId.value = record.draftId;
    revision.value = record.revision;
    graph.value = restoreBpmGraphDraft(record);
    baseline.value = cloneGraphDocument(graph.value);
    await router.replace({ query: { draftId: String(record.draftId) } });
    ElMessage.success('Graph 草稿已创建');
  } finally {
    busy.value = false;
  }
}

async function saveDraft() {
  if (!draftId.value || !revision.value) {
    await createDraft();
    return;
  }
  busy.value = true;
  try {
    const record = await saveBpmGraphDraft({
      draftId: draftId.value,
      graph: graph.value,
      revision: revision.value,
    });
    revision.value = record.revision;
    graph.value = restoreBpmGraphDraft(record);
    baseline.value = cloneGraphDocument(graph.value);
    ElMessage.success('Graph 草稿已保存');
  } finally {
    busy.value = false;
  }
}

async function publishDraft() {
  if (!draftId.value) {
    ElMessage.warning('请先创建并保存 Graph 草稿');
    return;
  }
  if (dirty.value) {
    ElMessage.warning('请先保存当前 Graph 再发布');
    return;
  }
  if (!simulation.value.pass) {
    ElMessage.warning('请先修正模拟诊断中的阻断项');
    return;
  }
  busy.value = true;
  try {
    const versionId = await publishBpmGraphDefinition(draftId.value);
    await loadDefinitionDetail(versionId);
    definitionDetailVisible.value = true;
    ElMessage.success(`Graph 定义已发布为版本 ${versionId}`);
  } finally {
    busy.value = false;
  }
}

async function loadDefinitionDetail(versionId: number) {
  publishedDefinition.value = await getBpmGraphDefinitionDetail(versionId);
}

async function openDefinitionDetail() {
  if (!publishedDefinition.value) {
    return;
  }
  busy.value = true;
  try {
    await loadDefinitionDetail(publishedDefinition.value.graphDefinitionVersionId);
    definitionDetailVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Graph 定义版本加载失败');
  } finally {
    busy.value = false;
  }
}

async function deactivatePublishedDefinition() {
  const definition = publishedDefinition.value;
  if (!definition || definition.lifecycleState !== 'ACTIVE') {
    return;
  }
  await ElMessageBox.confirm('下线后该定义版本不能继续发起，是否继续？', '下线 Graph 定义', {
    confirmButtonText: '下线',
    type: 'warning',
  });
  busy.value = true;
  try {
    await deactivateBpmGraphDefinition(definition.graphDefinitionVersionId);
    await loadDefinitionDetail(definition.graphDefinitionVersionId);
    ElMessage.success('Graph 定义已下线');
  } finally {
    busy.value = false;
  }
}

function backToModelList() {
  void router.push('/system/bpm/model');
}

watch(
  () => route.query.draftId,
  () => {
    const targetDraftId = readDraftId();
    if (!targetDraftId || targetDraftId === draftId.value) {
      return;
    }
    void loadDraft(targetDraftId).catch((error) => {
      ElMessage.error(error?.message || 'Graph 草稿加载失败');
    });
  },
  { immediate: true },
);
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="graph-editor-page">
      <header class="graph-editor-page__header">
        <div>
          <ElButton :icon="ArrowLeft" link type="primary" @click="backToModelList">返回模型列表</ElButton>
          <h1>流程 Graph 设计器</h1>
        </div>
        <div class="graph-editor-page__actions">
          <ElTag :type="simulation.pass ? 'success' : 'danger'">
            {{ simulation.pass ? '模拟通过' : `${simulation.findings.length} 个发布阻断项` }}
          </ElTag>
          <ElTag :type="dirty ? 'warning' : 'success'">
            {{ dirty ? '存在未保存语义变更' : '语义已保存' }}
          </ElTag>
          <ElButton :disabled="busy" :icon="Check" type="primary" @click="saveDraft">保存草稿</ElButton>
          <ElButton :disabled="busy || !hasDraft" :icon="CircleCheckBig" type="success" @click="publishDraft">发布定义</ElButton>
          <ElButton v-if="publishedDefinition" :disabled="busy" :icon="Search" @click="openDefinitionDetail">
            查看版本 v{{ publishedDefinition.definitionVersion }}
          </ElButton>
        </div>
      </header>

      <section v-if="!hasDraft" class="graph-editor-page__create">
        <ElInput v-model="draftMeta.processKey" placeholder="流程编码，例如 expense-approval" />
        <ElInput v-model="draftMeta.processName" placeholder="流程名称" />
        <ElInputNumber v-model="draftMeta.categoryId" :min="1" placeholder="分类 ID（可选）" />
        <ElButton :disabled="busy" :icon="Plus" type="primary" @click="createDraft">创建 Graph 草稿</ElButton>
      </section>

      <GraphProcessDesigner v-model="graph" :baseline="baseline" :disabled="busy" />

      <ElDialog
        v-model="definitionDetailVisible"
        :close-on-click-modal="false"
        title="Graph 定义版本"
        width="min(1100px, 96vw)"
      >
        <template v-if="publishedDefinition">
          <div class="graph-editor-page__definition-meta">
            <ElTag>{{ publishedDefinition.processKey }} v{{ publishedDefinition.definitionVersion }}</ElTag>
            <ElTag :type="publishedDefinition.lifecycleState === 'ACTIVE' ? 'success' : 'info'">
              {{ publishedDefinition.lifecycleState === 'ACTIVE' ? '已发布' : '已下线' }}
            </ElTag>
            <span>引擎定义：{{ publishedDefinition.engineProcessDefinitionId }}</span>
            <span>映射：{{ publishedDefinition.mappings.length }}</span>
          </div>
          <ElTabs>
            <ElTabPane label="编译 BPMN">
              <ElInput :model-value="publishedDefinition.compiledBpmnXml" :rows="18" readonly type="textarea" />
            </ElTabPane>
            <ElTabPane label="元素映射">
              <ElTable :data="publishedDefinition.mappings" max-height="420" size="small">
                <ElTableColumn label="作者元素" min-width="180" prop="authoredElementId" />
                <ElTableColumn label="作者类型" min-width="120" prop="authoredElementKind" />
                <ElTableColumn label="编译元素" min-width="220" prop="compiledElementId" />
                <ElTableColumn label="编译类型" min-width="140" prop="compiledElementType" />
              </ElTable>
            </ElTabPane>
            <ElTabPane label="冻结信息">
              <ElInput :model-value="publishedDefinition.semanticHash" readonly>
                <template #prepend>语义哈希</template>
              </ElInput>
              <ElInput :model-value="publishedDefinition.dependencyVersionsJson" :rows="8" readonly type="textarea" />
            </ElTabPane>
          </ElTabs>
        </template>
        <template #footer>
          <ElButton
            v-if="publishedDefinition?.lifecycleState === 'ACTIVE'"
            :disabled="busy"
            type="danger"
            @click="deactivatePublishedDefinition"
          >
            下线版本
          </ElButton>
          <ElButton @click="definitionDetailVisible = false">关闭</ElButton>
        </template>
      </ElDialog>
    </div>
  </Page>
</template>

<style scoped>
.graph-editor-page { display: flex; flex-direction: column; height: 100%; min-height: 0; gap: 12px; }
.graph-editor-page__header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.graph-editor-page__header h1 { margin: 2px 0 0; font-size: 18px; font-weight: 600; }
.graph-editor-page__actions, .graph-editor-page__create { display: flex; align-items: center; gap: 8px; }
.graph-editor-page__create { padding: 12px; border: 1px solid var(--el-border-color); background: var(--el-fill-color-lighter); }
.graph-editor-page__create :deep(.el-input), .graph-editor-page__create :deep(.el-input-number) { width: 240px; }
.graph-editor-page__definition-meta { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 12px; color: var(--el-text-color-secondary); font-size: 13px; }
@media (max-width: 900px) { .graph-editor-page__header, .graph-editor-page__actions, .graph-editor-page__create { align-items: stretch; flex-direction: column; } .graph-editor-page__create :deep(.el-input), .graph-editor-page__create :deep(.el-input-number) { width: 100%; } }
</style>
