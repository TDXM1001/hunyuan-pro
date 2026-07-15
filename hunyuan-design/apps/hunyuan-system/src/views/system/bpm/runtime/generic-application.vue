<script setup lang="ts">
import type { BpmBusinessObjectDetail, BpmBusinessObjectField, BpmBusinessObjectSummary, BpmStartableDefinitionRecord } from '#/api/system/bpm';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Delete, Plus } from '@element-plus/icons-vue';
import { Page } from '@vben/common-ui';
import { ElButton, ElCard, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElOption, ElSelect, ElTable, ElTableColumn, ElUpload } from 'element-plus';
import { getBpmBusinessObjectDetail, queryBpmStartableDefinitions, queryGenericApplicationContracts, submitBpmGenericApplication } from '#/api/system/bpm';
import { toBusinessObjectFormRules } from '#/components/bpm/business-object/business-object-form-rules';
import BpmRuntimeFormRenderer from './components/bpm-runtime-form-renderer.vue';

const router = useRouter();
const loading = ref(false);
const submitting = ref(false);
const contracts = ref<BpmBusinessObjectSummary[]>([]);
const definitions = ref<BpmStartableDefinitionRecord[]>([]);
const detail = ref<BpmBusinessObjectDetail>();
const fields = ref<Record<string, any>>({});
const routingFacts = ref<Record<string, any>>({});
const lineItems = ref<Record<string, any>[]>([]);
const attachments = ref<Record<string, any>[]>([]);
const form = reactive({ businessKey: '', contractIdentity: '', graphDefinitionVersionId: undefined as number | undefined, summary: '', title: '' });
const selectedContract = computed(() => contracts.value.find((item) => `${item.contractKey}@${item.contractVersion}` === form.contractIdentity));
const applicantRules = computed(() => detail.value ? toBusinessObjectFormRules(detail.value, 'APPLICANT') : []);
const routingRules = computed(() => rulesFor(detail.value?.configuration?.routingFacts || []));
function rulesFor(fieldSchema: BpmBusinessObjectField[]) { if (!detail.value?.configuration) return []; return toBusinessObjectFormRules({ ...detail.value, configuration: { ...detail.value.configuration, fieldSchema } }, 'APPLICANT'); }
function rulesJson(rules: unknown[]) { return JSON.stringify(rules); }
function addLine() { lineItems.value.push({}); }
function selectFiles(file: any) { attachments.value = [...attachments.value, { fileName: file.name, sizeMb: Number((file.size / 1024 / 1024).toFixed(2)) }]; }

watch(selectedContract, async (contract) => {
  detail.value = contract ? await getBpmBusinessObjectDetail(contract.contractKey, contract.contractVersion) : undefined;
  fields.value = {}; routingFacts.value = {}; lineItems.value = []; attachments.value = [];
  const minRows = detail.value?.configuration?.lineItemSchema?.minRows || 0;
  for (let index = 0; index < minRows; index++) addLine();
});

async function loadOptions() {
  loading.value = true;
  try {
    const [contractRows, definitionRows] = await Promise.all([queryGenericApplicationContracts(), queryBpmStartableDefinitions()]);
    contracts.value = contractRows || [];
    definitions.value = (definitionRows || []).filter((item) => item.definitionSource === 'GRAPH' && item.graphDefinitionVersionId);
  } finally { loading.value = false; }
}

async function submit() {
  const contract = selectedContract.value;
  const configuration = detail.value?.configuration;
  if (!contract || !configuration || !form.graphDefinitionVersionId || !form.businessKey.trim() || !form.title.trim()) { ElMessage.warning('请选择流程和业务对象，并填写业务编号与标题'); return; }
  submitting.value = true;
  try {
    const result = await submitBpmGenericApplication({ attachments: attachments.value, businessKey: form.businessKey.trim(), businessType: configuration.businessType, contractKey: contract.contractKey, contractVersion: contract.contractVersion, fields: fields.value, graphDefinitionVersionId: form.graphDefinitionVersionId, lineItems: lineItems.value, routingFacts: routingFacts.value, sourceSystem: configuration.sourceSystem, summary: form.summary.trim(), title: form.title.trim(), workingData: {} });
    ElMessage.success(`申请已发起，实例 ${result.instanceId}`);
    await router.push('/system/bpm/runtime/my-instance-list');
  } finally { submitting.value = false; }
}
onMounted(() => void loadOptions());
</script>

<template><Page auto-content-height content-class="!p-3 overflow-auto"><div v-loading="loading" class="generic-application"><ElCard shadow="never"><ElForm label-position="top"><div class="grid"><ElFormItem label="流程定义" required><ElSelect v-model="form.graphDefinitionVersionId" filterable placeholder="选择已发布流程"><ElOption v-for="definition in definitions" :key="definition.graphDefinitionVersionId!" :label="`${definition.definitionName} v${definition.definitionVersion}`" :value="definition.graphDefinitionVersionId!"/></ElSelect></ElFormItem><ElFormItem label="业务对象" required><ElSelect v-model="form.contractIdentity" filterable placeholder="选择已启用业务对象"><ElOption v-for="contract in contracts" :key="`${contract.contractKey}-${contract.contractVersion}`" :label="`${contract.objectName} v${contract.contractVersion}`" :value="`${contract.contractKey}@${contract.contractVersion}`"/></ElSelect></ElFormItem><ElFormItem label="业务编号" required><ElInput v-model="form.businessKey" :placeholder="detail?.configuration ? `${detail.configuration.businessKeyRule.prefix}-日期-流水号` : ''"/></ElFormItem><ElFormItem label="审批标题" required><ElInput v-model="form.title"/></ElFormItem></div><ElFormItem label="审批摘要"><ElInput v-model="form.summary" :rows="3" type="textarea"/></ElFormItem></ElForm></ElCard><ElCard v-if="detail" shadow="never"><h3>申请信息</h3><BpmRuntimeFormRenderer v-model="fields" :schema-json="rulesJson(applicantRules)"/></ElCard><ElCard v-if="detail?.configuration?.routingFacts.length" shadow="never"><h3>审批路由信息</h3><BpmRuntimeFormRenderer v-model="routingFacts" :schema-json="rulesJson(routingRules)"/></ElCard><ElCard v-if="detail?.configuration?.lineItemSchema" shadow="never"><header><h3>{{detail.configuration.lineItemSchema.name}}</h3><ElButton :icon="Plus" :disabled="lineItems.length>=detail.configuration.lineItemSchema.maxRows" @click="addLine">新增一行</ElButton></header><ElTable :data="lineItems"><ElTableColumn v-for="field in detail.configuration.lineItemSchema.fields" :key="field.key" :label="field.label" min-width="150"><template #default="{row}"><ElInputNumber v-if="['DECIMAL','INTEGER'].includes(field.type)" v-model="row[field.key]"/><ElInput v-else v-model="row[field.key]"/></template></ElTableColumn><ElTableColumn label="操作" width="70"><template #default="{$index}"><ElButton :disabled="lineItems.length<=detail!.configuration!.lineItemSchema!.minRows" :icon="Delete" link type="danger" @click="lineItems.splice($index,1)"/></template></ElTableColumn></ElTable></ElCard><ElCard v-if="detail" shadow="never"><h3>附件</h3><ElUpload :auto-upload="false" :show-file-list="false" multiple @change="selectFiles"><ElButton>选择文件</ElButton></ElUpload><div class="attachments"><span v-for="(attachment,index) in attachments" :key="`${attachment.fileName}-${index}`">{{attachment.fileName}}（{{attachment.sizeMb}} MB）<ElButton :icon="Delete" link type="danger" @click="attachments.splice(index,1)"/></span></div></ElCard><div class="actions"><ElButton :loading="submitting" type="primary" @click="submit">提交申请</ElButton></div></div></Page></template>
<style scoped>.generic-application{display:grid;gap:12px;max-width:1120px;margin:0 auto}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 16px}.generic-application :deep(.el-select),.generic-application :deep(.el-input-number){width:100%}header{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}h3{margin:0 0 16px;font-size:16px;line-height:24px;font-weight:600;letter-spacing:0}header h3{margin:0}.attachments{display:grid;gap:6px;margin-top:10px}.actions{display:flex;justify-content:flex-end;padding-bottom:16px}@media(max-width:720px){.grid{grid-template-columns:1fr}}</style>
