<script setup lang="ts">
import type { EmployeeRecord } from '#/api/system/organization';

import type {
  BpmProcessDesignerExpose,
  BpmProcessDesignerSnapshot,
  BpmProcessNodeDraft,
} from './types';

import 'bpmn-js/dist/assets/diagram-js.css';

import BpmnNavigatedViewer from 'bpmn-js/lib/NavigatedViewer';
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

import {
  ElButton,
  ElCard,
  ElFormItem,
  ElInput,
  ElOption,
  ElSelect,
  ElSpace,
  ElSwitch,
  ElTable,
  ElTableColumn,
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
const viewer = ref<any>();
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
const formFieldOptions = computed(() => extractFormFieldOptions(props.formSchemaJson));

function extractFormFieldOptions(schemaJson: string) {
  const result: { field: string; label: string; type: string }[] = [];
  const visit = (value: unknown) => {
    if (Array.isArray(value)) {
      value.forEach(visit);
      return;
    }
    if (!value || typeof value !== 'object') {
      return;
    }
    const item = value as Record<string, any>;
    if (typeof item.field === 'string' && item.field.trim()) {
      result.push({
        field: item.field.trim(),
        label: String(item.title || item.label || item.field),
        type: String(item.type || item.component || '-'),
      });
    }
    visit(item.fields);
    visit(item.children);
  };
  try {
    visit(JSON.parse(schemaJson || '[]'));
  } catch {
    return [];
  }
  return result;
}

function getFieldPermission(fieldKey: string) {
  return selectedNode.value?.fieldPermissions?.find(
    (item) => item.fieldKey === fieldKey,
  );
}

function getFieldPermissionMode(fieldKey: string) {
  return getFieldPermission(fieldKey)?.permission || 'READONLY';
}

function setFieldPermissionMode(
  fieldKey: string,
  permission: 'EDITABLE' | 'HIDDEN' | 'READONLY',
) {
  if (!selectedNode.value) {
    return;
  }
  const current = getFieldPermission(fieldKey);
  const next = {
    fieldKey,
    permission,
    required: permission === 'EDITABLE' && Boolean(current?.required),
  };
  selectedNode.value.fieldPermissions = [
    ...(selectedNode.value.fieldPermissions ?? []).filter(
      (item) => item.fieldKey !== fieldKey,
    ),
    next,
  ];
  void handleStateChange();
}

function setFieldRequired(fieldKey: string, required: boolean) {
  if (!selectedNode.value) {
    return;
  }
  const permission = getFieldPermissionMode(fieldKey);
  selectedNode.value.fieldPermissions = [
    ...(selectedNode.value.fieldPermissions ?? []).filter(
      (item) => item.fieldKey !== fieldKey,
    ),
    { fieldKey, permission, required: permission === 'EDITABLE' && required },
  ];
  void handleStateChange();
}

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

function ensureViewer() {
  if (viewer.value || !canvasRef.value) {
    return;
  }

  viewer.value = new BpmnNavigatedViewer({
    container: canvasRef.value,
  });
}

async function renderCanvas() {
  ensureViewer();

  const xml = buildReadonlyBpmnXml(
    props.modelKey || 'process_model',
    props.modelName || '流程模型',
    nodes.value,
  );

  await nextTick();
  await viewer.value?.importXML?.(xml);
  await viewer.value?.get?.('canvas')?.zoom?.('fit-viewport');
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
        (employeeId) => !Number.isSafeInteger(employeeId) || employeeId <= 0,
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

  const knownFields = new Set(formFieldOptions.value.map((item) => item.field));
  const invalidPermissionNode = nodes.value.find((node) =>
    (node.fieldPermissions ?? []).some(
      (permission) =>
        !knownFields.has(permission.fieldKey) ||
        (permission.required && permission.permission !== 'EDITABLE') ||
        (node.approvalMode === 'parallelAll' &&
          permission.permission === 'EDITABLE'),
    ),
  );
  if (invalidPermissionNode) {
    return {
      message: `审批节点【${invalidPermissionNode.name}】字段权限配置不合法，请检查表单字段和审批模式`,
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
      new Map(mergedOptions.map((item) => [item.employeeId, item])).values(),
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
  if (selectedNode.value.candidateResolverType !== 'EMPLOYEE_SELECT_AT_START') {
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
  ensureViewer();
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
  viewer.value?.destroy?.();
  viewer.value = undefined;
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
            v-if="
              selectedNode.candidateResolverType === 'EMPLOYEE_SELECT_AT_START'
            "
            label="自选字段"
          >
            <ElSelect
              v-model="selectedNode.employeeSelectFieldKey"
              :disabled="
                disabled || readonly || !employeeSelectFieldOptions.length
              "
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
          <ElFormItem label="字段权限">
            <ElTable
              v-if="formFieldOptions.length"
              :data="formFieldOptions"
              size="small"
              style="width: 100%"
            >
              <ElTableColumn label="字段" min-width="118">
                <template #default="{ row }">
                  <div>{{ row.label }}</div>
                  <small>{{ row.field }}</small>
                </template>
              </ElTableColumn>
              <ElTableColumn label="权限" width="116">
                <template #default="{ row }">
                  <ElSelect
                    :disabled="disabled || readonly"
                    :model-value="getFieldPermissionMode(row.field)"
                    size="small"
                    @change="(value) => setFieldPermissionMode(row.field, value)"
                  >
                    <ElOption label="只读" value="READONLY" />
                    <ElOption
                      :disabled="selectedNode.approvalMode === 'parallelAll'"
                      label="可编辑"
                      value="EDITABLE"
                    />
                    <ElOption label="隐藏" value="HIDDEN" />
                  </ElSelect>
                </template>
              </ElTableColumn>
              <ElTableColumn label="必填" width="62" align="center">
                <template #default="{ row }">
                  <ElSwitch
                    :disabled="
                      disabled ||
                      readonly ||
                      getFieldPermissionMode(row.field) !== 'EDITABLE'
                    "
                    :model-value="Boolean(getFieldPermission(row.field)?.required)"
                    @change="(value) => setFieldRequired(row.field, Boolean(value))"
                  />
                </template>
              </ElTableColumn>
            </ElTable>
            <span v-else>当前模型未绑定可配置字段的表单</span>
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
              :model-value="
                JSON.stringify(selectedNode.listeners || [], null, 2)
              "
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
  height: 100%;
  min-height: 0;
}

.bpm-process-designer-adapter__toolbar {
  align-items: center;
  display: flex;
  flex: 0 0 auto;
  justify-content: space-between;
}

.bpm-process-designer-adapter__body {
  display: grid;
  flex: 1;
  gap: 12px;
  grid-template-columns: minmax(0, 1fr) 320px;
  min-height: 0;
  overflow: hidden;
}

.bpm-process-designer-adapter__canvas {
  background-color: var(--el-bg-color);
  background-image:
    linear-gradient(var(--el-border-color-extra-light) 1px, transparent 1px),
    linear-gradient(
      90deg,
      var(--el-border-color-extra-light) 1px,
      transparent 1px
    );
  background-size: 20px 20px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  min-height: 0;
  overflow: hidden;
}

.bpm-process-designer-adapter__panel {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  min-height: 0;
  overflow: hidden;
}

.bpm-process-designer-adapter__panel :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: calc(100% - 57px);
  overflow-y: auto;
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
    overflow-y: auto;
  }

  .bpm-process-designer-adapter__canvas {
    min-height: 420px;
  }

  .bpm-process-designer-adapter__panel {
    min-height: 420px;
  }
}
</style>
