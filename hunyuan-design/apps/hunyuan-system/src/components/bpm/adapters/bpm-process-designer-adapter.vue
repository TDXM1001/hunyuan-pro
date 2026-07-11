<script setup lang="ts">
import type { EmployeeRecord } from '#/api/system/organization';

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

import { queryEmployeePage } from '#/api/system/organization';

import {
  buildReadonlyBpmnXml,
  parseSimpleModelDraft,
} from './simple-model-bridge';
import { extractEmployeeSelectFieldOptions } from './employee-select-field-options';

defineOptions({ name: 'BpmProcessDesignerAdapter' });

const emit = defineEmits<{
  change: [snapshot: BpmProcessDesignerSnapshot];
  ready: [];
}>();

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    formSchemaJson?: string;
    initialSnapshot?: Partial<BpmProcessDesignerSnapshot>;
    modelKey?: string;
    modelName?: string;
    readonly?: boolean;
  }>(),
  {
    disabled: false,
    formSchemaJson: '',
    initialSnapshot: () => ({}),
    modelKey: '',
    modelName: '',
    readonly: false,
  },
);

const canvasRef = ref<HTMLDivElement>();
const dirty = ref(false);
const employeeLoading = ref(false);
const employeeOptions = ref<EmployeeRecord[]>([]);
const modeler = ref<any>();
const nodes = ref<BpmProcessNodeDraft[]>([]);
const selectedNodeId = ref('');

const selectedNode = computed(() =>
  nodes.value.find((item) => item.nodeKey === selectedNodeId.value),
);
const employeeSelectFieldOptions = computed(() =>
  extractEmployeeSelectFieldOptions(props.formSchemaJson),
);
const employeeSelectFieldSet = computed(
  () => new Set(employeeSelectFieldOptions.value.map((item) => item.field)),
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
  if (
    nodes.value.some(
      (item) =>
        item.approvalMode === 'sequential' ||
        item.approvalMode === 'parallelAll',
    )
  ) {
    await loadEmployeeOptions();
  }
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
  if (!nodes.value.length) {
    return {
      message: '请至少保留一个审批节点',
      ok: false,
    };
  }

  const invalidEmployeeSelectNode = nodes.value.find(
    (item) =>
      item.candidateResolverType === 'EMPLOYEE_SELECT_AT_START' &&
      (!item.employeeSelectFieldKey ||
        !employeeSelectFieldSet.value.has(item.employeeSelectFieldKey)),
  );
  if (invalidEmployeeSelectNode) {
    return {
      message: `审批节点【${invalidEmployeeSelectNode.name}】请选择发起时自选审批人字段`,
      ok: false,
    };
  }

  const invalidMultipleResolverNode = nodes.value.find(
    (item) =>
      (item.approvalMode === 'sequential' ||
        item.approvalMode === 'parallelAll') &&
      item.candidateResolverType !== 'EMPLOYEE',
  );
  if (invalidMultipleResolverNode) {
    const modeLabel =
      invalidMultipleResolverNode.approvalMode === 'parallelAll'
        ? '并行全员会签'
        : '顺序审批';
    return {
      message: `审批节点【${invalidMultipleResolverNode.name}】${modeLabel}仅支持指定员工`,
      ok: false,
    };
  }

  const invalidMultipleEmployeesNode = nodes.value.find((item) => {
    if (
      item.approvalMode !== 'sequential' &&
      item.approvalMode !== 'parallelAll'
    ) {
      return false;
    }
    const employeeIds = item.employeeIds ?? [];
    return (
      employeeIds.length < 2 ||
      employeeIds.some(
        (employeeId) =>
          !Number.isSafeInteger(employeeId) || employeeId <= 0,
      ) ||
      new Set(employeeIds).size !== employeeIds.length
    );
  });
  if (invalidMultipleEmployeesNode) {
    const modeLabel =
      invalidMultipleEmployeesNode.approvalMode === 'parallelAll'
        ? '并行会签'
        : '顺序审批';
    return {
      message: `审批节点【${invalidMultipleEmployeesNode.name}】请至少选择 2 名不同的${modeLabel}员工`,
      ok: false,
    };
  }

  return {
    message: undefined,
    ok: true,
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

async function loadEmployeeOptions(keyword = '') {
  employeeLoading.value = true;
  try {
    const result = await queryEmployeePage({
      disabledFlag: false,
      keyword,
      pageNum: 1,
      pageSize: 20,
    });
    const selectedEmployeeIds = new Set(
      nodes.value.flatMap((item) => item.employeeIds ?? []),
    );
    const selectedOptions = employeeOptions.value.filter((item) =>
      selectedEmployeeIds.has(item.employeeId),
    );
    const mergedOptions = [...selectedOptions, ...(result?.list ?? [])];
    employeeOptions.value = Array.from(
      new Map(
        mergedOptions.map((item) => [item.employeeId, item]),
      ).values(),
    );
  } finally {
    employeeLoading.value = false;
  }
}

async function handleCandidateResolverChange() {
  if (!selectedNode.value) {
    return;
  }
  if (
    (selectedNode.value.approvalMode === 'sequential' ||
      selectedNode.value.approvalMode === 'parallelAll') &&
    selectedNode.value.candidateResolverType !== 'EMPLOYEE'
  ) {
    selectedNode.value.approvalMode = 'single';
    selectedNode.value.employeeIds = undefined;
  }
  if (
    selectedNode.value.candidateResolverType !== 'EMPLOYEE_SELECT_AT_START'
  ) {
    selectedNode.value.employeeSelectFieldKey = undefined;
  }
  await handleStateChange();
}

async function handleApprovalModeChange() {
  if (!selectedNode.value) {
    return;
  }
  if (
    selectedNode.value.approvalMode === 'sequential' ||
    selectedNode.value.approvalMode === 'parallelAll'
  ) {
    selectedNode.value.candidateResolverType = 'EMPLOYEE';
    selectedNode.value.employeeSelectFieldKey = undefined;
    selectedNode.value.employeeIds ??= [];
    await loadEmployeeOptions();
  } else {
    selectedNode.value.employeeIds = undefined;
  }
  await handleStateChange();
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
              :disabled="
                disabled ||
                readonly ||
                selectedNode.approvalMode === 'sequential' ||
                selectedNode.approvalMode === 'parallelAll'
              "
              @change="handleCandidateResolverChange"
            >
              <ElOption label="指定员工" value="EMPLOYEE" />
              <ElOption label="部门负责人" value="DEPARTMENT_MANAGER" />
              <ElOption label="发起人本人" value="START_EMPLOYEE" />
              <ElOption
                label="发起时自选审批人"
                value="EMPLOYEE_SELECT_AT_START"
              />
              <ElOption
                label="发起人部门主管"
                value="START_DEPARTMENT_MANAGER"
              />
              <ElOption label="角色" value="ROLE" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem
            v-if="selectedNode.candidateResolverType === 'EMPLOYEE_SELECT_AT_START'"
            label="自选字段"
          >
            <ElSelect
              v-model="selectedNode.employeeSelectFieldKey"
              :disabled="disabled || readonly || !employeeSelectFieldOptions.length"
              placeholder="请选择表单中的员工字段"
              @change="handleStateChange"
            >
              <ElOption
                v-for="field in employeeSelectFieldOptions"
                :key="field.field"
                :label="field.label"
                :value="field.field"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="审批模式">
            <ElSelect
              v-model="selectedNode.approvalMode"
              :disabled="disabled || readonly"
              @change="handleApprovalModeChange"
            >
              <ElOption label="单人审批" value="single" />
              <ElOption label="单人审批（严格）" value="singleOnly" />
              <ElOption label="顺序多人审批" value="sequential" />
              <ElOption label="并行全员会签" value="parallelAll" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem
            v-if="
              selectedNode.approvalMode === 'sequential' ||
              selectedNode.approvalMode === 'parallelAll'
            "
            :label="
              selectedNode.approvalMode === 'parallelAll'
                ? '并行会签员工'
                : '顺序审批员工'
            "
          >
            <ElSelect
              v-model="selectedNode.employeeIds"
              clearable
              collapse-tags
              :disabled="disabled || readonly"
              filterable
              :loading="employeeLoading"
              multiple
              :placeholder="
                selectedNode.approvalMode === 'parallelAll'
                  ? '请选择全部会签员工'
                  : '请按审批顺序选择员工'
              "
              remote
              :remote-method="loadEmployeeOptions"
              @change="handleStateChange"
            >
              <ElOption
                v-for="employee in employeeOptions"
                :key="employee.employeeId"
                :label="`${employee.actualName}（${employee.departmentName || '未分配部门'}）`"
                :value="employee.employeeId"
              />
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
