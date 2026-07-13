<script setup lang="ts">
import type {
  BpmBusinessContractRecord,
  BpmStartableDefinitionRecord,
} from '#/api/system/bpm';

import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import {
  ElButton,
  ElCard,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElSelect,
} from 'element-plus';

import {
  queryBpmStartableDefinitions,
  queryGenericApplicationContracts,
  submitBpmGenericApplication,
} from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmGenericApplication' });

interface ContractField {
  key: string;
  required?: boolean;
  sensitivity?: string;
  type: 'BOOLEAN' | 'DECIMAL' | 'EMPLOYEE_ID' | 'INTEGER' | 'STRING';
}

interface ContractDocument {
  businessType: string;
  fieldSchema: ContractField[];
  routingFacts: ContractField[];
  sourceSystem: string;
  workingDataSchema: ContractField[];
}

const router = useRouter();
const loading = ref(false);
const submitting = ref(false);
const contracts = ref<BpmBusinessContractRecord[]>([]);
const definitions = ref<BpmStartableDefinitionRecord[]>([]);
const fields = reactive<Record<string, any>>({});
const routingFacts = reactive<Record<string, any>>({});
const workingData = reactive<Record<string, any>>({});
const form = reactive({
  attachmentsJson: '[]',
  businessKey: '',
  contractIdentity: '',
  graphDefinitionVersionId: undefined as number | undefined,
  lineItemsJson: '[]',
  summary: '',
  title: '',
});

const selectedContract = computed(() => contracts.value.find((item) =>
  `${item.contractKey}@${item.contractVersion}` === form.contractIdentity,
));
const contractDocument = computed<ContractDocument | undefined>(() => {
  try {
    return selectedContract.value
      ? JSON.parse(selectedContract.value.canonicalContractJson) as ContractDocument
      : undefined;
  } catch {
    return undefined;
  }
});

function clearRecord(record: Record<string, any>) {
  Object.keys(record).forEach((key) => delete record[key]);
}

function initializeFields(schema: ContractField[] | undefined, target: Record<string, any>) {
  clearRecord(target);
  (schema || []).forEach((field) => {
    target[field.key] = field.type === 'BOOLEAN' ? false : undefined;
  });
}

watch(contractDocument, (document) => {
  initializeFields(document?.fieldSchema, fields);
  initializeFields(document?.routingFacts, routingFacts);
  initializeFields(document?.workingDataSchema, workingData);
});

function normalizeValue(field: ContractField, value: any) {
  if (value === '' || value === undefined) return null;
  if (['DECIMAL', 'EMPLOYEE_ID', 'INTEGER'].includes(field.type)) return Number(value);
  return value;
}

function normalizedData(schema: ContractField[] | undefined, source: Record<string, any>) {
  return Object.fromEntries((schema || []).map((field) => [field.key, normalizeValue(field, source[field.key])]));
}

async function loadOptions() {
  loading.value = true;
  try {
    const [contractRows, definitionRows] = await Promise.all([
      queryGenericApplicationContracts(),
      queryBpmStartableDefinitions(),
    ]);
    contracts.value = contractRows || [];
    definitions.value = (definitionRows || []).filter((item) =>
      item.definitionSource === 'GRAPH' && item.graphDefinitionVersionId,
    );
  } finally {
    loading.value = false;
  }
}

async function submit() {
  const contract = selectedContract.value;
  const document = contractDocument.value;
  if (!contract || !document || !form.graphDefinitionVersionId || !form.businessKey.trim() || !form.title.trim()) {
    ElMessage.warning('请选择流程和业务契约，并填写业务键与标题');
    return;
  }
  submitting.value = true;
  try {
    const result = await submitBpmGenericApplication({
      attachmentsJson: form.attachmentsJson,
      businessKey: form.businessKey.trim(),
      businessType: document.businessType,
      contractKey: contract.contractKey,
      contractVersion: contract.contractVersion,
      fieldsJson: JSON.stringify(normalizedData(document.fieldSchema, fields)),
      graphDefinitionVersionId: form.graphDefinitionVersionId,
      lineItemsJson: form.lineItemsJson,
      routingFactsJson: JSON.stringify(normalizedData(document.routingFacts, routingFacts)),
      sourceSystem: document.sourceSystem,
      summary: form.summary.trim(),
      title: form.title.trim(),
      workingDataJson: JSON.stringify(normalizedData(document.workingDataSchema, workingData)),
    });
    ElMessage.success(`申请已发起，实例 ${result.instanceId}`);
    await router.push('/system/bpm/runtime/my-instance-list');
  } finally {
    submitting.value = false;
  }
}

onMounted(() => void loadOptions());
</script>

<template>
  <Page auto-content-height content-class="!p-3 overflow-auto">
    <div v-loading="loading" class="generic-application">
      <ElCard shadow="never">
        <ElForm label-position="top">
          <div class="generic-application__grid">
            <ElFormItem label="流程定义" required>
              <ElSelect v-model="form.graphDefinitionVersionId" filterable placeholder="选择已发布 Graph 流程">
                <ElOption
                  v-for="definition in definitions"
                  :key="definition.graphDefinitionVersionId!"
                  :label="`${definition.definitionName} v${definition.definitionVersion}`"
                  :value="definition.graphDefinitionVersionId!"
                />
              </ElSelect>
            </ElFormItem>
            <ElFormItem label="业务契约" required>
              <ElSelect v-model="form.contractIdentity" filterable placeholder="选择已启用业务契约">
                <ElOption
                  v-for="contract in contracts"
                  :key="contract.businessContractVersionId || `${contract.contractKey}-${contract.contractVersion}`"
                  :label="`${contract.contractKey} v${contract.contractVersion}`"
                  :value="`${contract.contractKey}@${contract.contractVersion}`"
                />
              </ElSelect>
            </ElFormItem>
            <ElFormItem label="业务键" required><ElInput v-model="form.businessKey" placeholder="例如 REQ-2026-0001" /></ElFormItem>
            <ElFormItem label="审批标题" required><ElInput v-model="form.title" /></ElFormItem>
          </div>
          <ElFormItem label="审批摘要"><ElInput v-model="form.summary" :rows="3" type="textarea" /></ElFormItem>
        </ElForm>
      </ElCard>

      <ElCard v-if="contractDocument" shadow="never">
        <h3>审批对象字段</h3>
        <div class="generic-application__grid">
          <ElFormItem v-for="field in contractDocument.fieldSchema" :key="field.key" :label="field.key" :required="field.required">
            <ElInputNumber v-if="['DECIMAL', 'EMPLOYEE_ID', 'INTEGER'].includes(field.type)" v-model="fields[field.key]" controls-position="right" />
            <ElSelect v-else-if="field.type === 'BOOLEAN'" v-model="fields[field.key]"><ElOption label="是" :value="true" /><ElOption label="否" :value="false" /></ElSelect>
            <ElInput v-else v-model="fields[field.key]" />
          </ElFormItem>
        </div>
      </ElCard>

      <ElCard v-if="contractDocument" shadow="never">
        <h3>路由事实与工作数据</h3>
        <div class="generic-application__grid">
          <ElFormItem v-for="field in contractDocument.routingFacts" :key="`route-${field.key}`" :label="`路由：${field.key}`" :required="field.required">
            <ElInputNumber v-if="['DECIMAL', 'EMPLOYEE_ID', 'INTEGER'].includes(field.type)" v-model="routingFacts[field.key]" controls-position="right" />
            <ElInput v-else v-model="routingFacts[field.key]" />
          </ElFormItem>
          <ElFormItem v-for="field in contractDocument.workingDataSchema" :key="`work-${field.key}`" :label="`工作数据：${field.key}`" :required="field.required">
            <ElInputNumber v-if="['DECIMAL', 'EMPLOYEE_ID', 'INTEGER'].includes(field.type)" v-model="workingData[field.key]" controls-position="right" />
            <ElInput v-else v-model="workingData[field.key]" />
          </ElFormItem>
        </div>
      </ElCard>

      <ElCard v-if="contractDocument" shadow="never">
        <h3>明细与附件</h3>
        <ElForm label-position="top">
          <ElFormItem label="明细 JSON"><ElInput v-model="form.lineItemsJson" :rows="6" type="textarea" /></ElFormItem>
          <ElFormItem label="附件 JSON"><ElInput v-model="form.attachmentsJson" :rows="5" type="textarea" /></ElFormItem>
        </ElForm>
      </ElCard>

      <div class="generic-application__actions">
        <ElButton :loading="submitting" type="primary" @click="submit">提交申请</ElButton>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.generic-application { display: grid; gap: 12px; max-width: 1120px; margin: 0 auto; }
.generic-application__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }
.generic-application :deep(.el-select), .generic-application :deep(.el-input-number) { width: 100%; }
.generic-application h3 { margin: 0 0 16px; font-size: 16px; font-weight: 600; }
.generic-application__actions { display: flex; justify-content: flex-end; padding-bottom: 16px; }
@media (max-width: 720px) { .generic-application__grid { grid-template-columns: 1fr; } }
</style>
