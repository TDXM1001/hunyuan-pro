<script setup lang="ts">
import type {
  BpmBusinessContractDraftParams,
  BpmBusinessContractLifecycleState,
  BpmBusinessContractRecord,
} from '#/api/system/bpm';
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, onMounted, reactive, ref } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import { ArtTable, ArtTableHeader, ArtTablePanel, useTableColumns } from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';
import {
  ElButton,
  ElCard,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTag,
} from 'element-plus';

import {
  activateBpmBusinessContract,
  copyBpmBusinessContractAsDraft,
  createBpmBusinessContractDraft,
  queryBpmBusinessContracts,
  retireBpmBusinessContract,
  validateBpmBusinessContract,
} from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmBusinessContractCatalog' });

const defaultContract = JSON.stringify({
  sourceSystem: 'HUNYUAN',
  businessType: 'GENERIC_APPLICATION',
  businessKeyRule: { pattern: 'REQ-[0-9]{4}-[0-9]{4}' },
  fieldSchema: [
    { key: 'amount', type: 'DECIMAL', required: true, sensitivity: 'INTERNAL' },
    { key: 'applicantNote', type: 'STRING', required: false, sensitivity: 'INTERNAL' },
  ],
  routingFacts: [
    { key: 'financeApprover', type: 'EMPLOYEE_ID', required: true, sensitivity: 'INTERNAL', candidateUsable: true },
  ],
  workingDataSchema: [
    { key: 'approvedAmount', type: 'DECIMAL', required: true, sensitivity: 'INTERNAL' },
    { key: 'approvalNote', type: 'STRING', required: false, sensitivity: 'INTERNAL' },
  ],
  attachmentRules: { maxCount: 5 },
  detailLayout: { sections: ['fields', 'lineItems', 'attachments'] },
  changePolicy: { mode: 'FIELD_CONTROLLED', editableFields: ['approvedAmount', 'approvalNote'] },
}, null, 2);

const loading = ref(false);
const saving = ref(false);
const showSearchBar = ref(true);
const draftVisible = ref(false);
const detailVisible = ref(false);
const rows = ref<BpmBusinessContractRecord[]>([]);
const selected = ref<BpmBusinessContractRecord>();
const canonicalResult = ref('');
const filter = reactive<{ contractKey: string; lifecycleState: '' | BpmBusinessContractLifecycleState }>({
  contractKey: '', lifecycleState: '',
});
const draft = reactive<BpmBusinessContractDraftParams>({
  contractJson: defaultContract, contractKey: '', schemaVersion: 1,
});

const columnsFactory = (): ColumnOption<BpmBusinessContractRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 68, align: 'center' },
  { prop: 'contractKey', label: '契约编码', minWidth: 190 },
  { prop: 'contractVersion', label: '版本', width: 82, align: 'center', useSlot: true },
  { prop: 'lifecycleState', label: '状态', width: 110, useSlot: true },
  { prop: 'schemaVersion', label: 'Schema', width: 90, align: 'center' },
  { prop: 'contractDigest', label: '内容摘要', minWidth: 260, useSlot: true },
  { prop: 'operation', label: '操作', width: 260, fixed: 'right', useSlot: true },
];
const { columns, columnChecks } = useTableColumns(columnsFactory);
const tableHeight = computed(() => '100%');

function stateType(state: BpmBusinessContractLifecycleState) {
  return state === 'ACTIVE' ? 'success' : state === 'DRAFT' ? 'warning' : 'info';
}

async function loadData() {
  loading.value = true;
  try {
    rows.value = await queryBpmBusinessContracts({
      contractKey: filter.contractKey.trim() || undefined,
      lifecycleState: filter.lifecycleState || undefined,
    }) || [];
  } finally {
    loading.value = false;
  }
}

function reset() {
  Object.assign(filter, { contractKey: '', lifecycleState: '' });
  void loadData();
}

function openDraft() {
  Object.assign(draft, { contractJson: defaultContract, contractKey: '', schemaVersion: 1 });
  canonicalResult.value = '';
  draftVisible.value = true;
}

async function validateDraft() {
  if (!draft.contractKey.trim() || !draft.contractJson.trim()) {
    ElMessage.warning('请填写契约编码和契约内容');
    return;
  }
  const result = await validateBpmBusinessContract(draft);
  canonicalResult.value = result.canonicalContractJson;
  ElMessage.success('业务契约校验通过');
}

async function submitDraft() {
  saving.value = true;
  try {
    await createBpmBusinessContractDraft({ ...draft, contractKey: draft.contractKey.trim() });
    ElMessage.success('业务契约草稿已创建');
    draftVisible.value = false;
    await loadData();
  } finally {
    saving.value = false;
  }
}

async function copyAsDraft(row: BpmBusinessContractRecord) {
  await copyBpmBusinessContractAsDraft(row);
  ElMessage.success('已复制为新草稿版本');
  await loadData();
}

async function activate(row: BpmBusinessContractRecord) {
  await ElMessageBox.confirm('启用后该版本不可原地编辑，是否继续？', '启用业务契约', { type: 'warning' });
  await activateBpmBusinessContract(row);
  ElMessage.success('业务契约已启用');
  await loadData();
}

async function retire(row: BpmBusinessContractRecord) {
  await ElMessageBox.confirm('退休只阻止未来发布引用，既有实例仍使用冻结版本。是否继续？', '退休业务契约', { type: 'warning' });
  await retireBpmBusinessContract(row);
  ElMessage.success('业务契约已退休');
  await loadData();
}

function openDetail(row: BpmBusinessContractRecord) {
  selected.value = row;
  detailVisible.value = true;
}

onMounted(() => void loadData());
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="contract-page">
      <ElCard v-show="showSearchBar" shadow="never">
        <ArtSearchPanel :collapsible="false" :loading="loading" :show-refresh="false" @reset="reset" @search="loadData">
          <ElFormItem label="契约编码"><ElInput v-model="filter.contractKey" clearable /></ElFormItem>
          <ElFormItem label="状态">
            <ElSelect v-model="filter.lifecycleState" clearable>
              <ElOption label="草稿" value="DRAFT" />
              <ElOption label="已启用" value="ACTIVE" />
              <ElOption label="已退休" value="RETIRED" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>
      <ElCard class="table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader v-model="columnChecks" :loading="loading" layout="search,size,fullscreen,columns,settings" :show-search-bar="showSearchBar" @search="showSearchBar = !showSearchBar">
            <template #left><ElButton type="primary" @click="openDraft">新增契约草稿</ElButton></template>
          </ArtTableHeader>
          <ArtTable :columns="columns" :data="rows" :height="tableHeight" :loading="loading">
            <template #contractVersion="{ row }">v{{ row.contractVersion }}</template>
            <template #lifecycleState="{ row }"><ElTag :type="stateType(row.lifecycleState)" effect="plain">{{ row.lifecycleState }}</ElTag></template>
            <template #contractDigest="{ row }"><span class="digest">{{ row.contractDigest }}</span></template>
            <template #operation="{ row }">
              <ElButton link type="primary" @click="openDetail(row)">详情</ElButton>
              <ElButton link type="primary" @click="copyAsDraft(row)">复制</ElButton>
              <ElButton v-if="row.lifecycleState === 'DRAFT'" link type="success" @click="activate(row)">启用</ElButton>
              <ElButton v-if="row.lifecycleState === 'ACTIVE'" link type="danger" @click="retire(row)">退休</ElButton>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>

    <ElDialog v-model="draftVisible" destroy-on-close title="新建业务契约草稿" width="min(800px, 96vw)">
      <ElForm label-position="top" class="contract-form">
        <div class="contract-form__grid">
          <ElFormItem label="契约编码"><ElInput v-model="draft.contractKey" /></ElFormItem>
          <ElFormItem label="Schema 版本"><ElInput v-model.number="draft.schemaVersion" /></ElFormItem>
        </div>
        <ElFormItem label="契约 JSON"><ElInput v-model="draft.contractJson" :rows="18" type="textarea" /></ElFormItem>
        <ElFormItem v-if="canonicalResult" label="规范化结果"><ElInput :model-value="canonicalResult" :rows="9" readonly type="textarea" /></ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="draftVisible = false">取消</ElButton>
        <ElButton @click="validateDraft">校验</ElButton>
        <ElButton :loading="saving" type="primary" @click="submitDraft">创建草稿</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="detailVisible" title="业务契约版本详情" width="min(860px, 96vw)">
      <template v-if="selected">
        <div class="contract-detail__meta">
          <ElTag effect="plain">{{ selected.contractKey }} v{{ selected.contractVersion }}</ElTag>
          <ElTag :type="stateType(selected.lifecycleState)" effect="plain">{{ selected.lifecycleState }}</ElTag>
          <span>revision {{ selected.catalogRevision }}</span>
        </div>
        <ElInput :model-value="selected.canonicalContractJson" :rows="20" readonly type="textarea" />
      </template>
      <template #footer><ElButton @click="detailVisible = false">关闭</ElButton></template>
    </ElDialog>
  </Page>
</template>

<style scoped>
.contract-page { display: flex; flex-direction: column; gap: 12px; height: 100%; min-height: 0; }
.contract-page > .el-card:first-child { border: 0; flex-shrink: 0; }
.table-card { flex: 1; min-height: 0; overflow: hidden; }
.table-card :deep(.el-card__body) { display: flex; flex-direction: column; height: 100%; min-height: 0; padding: 16px; }
.contract-form__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }
.contract-form :deep(.el-select) { width: 100%; }
.contract-detail__meta { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; color: var(--el-text-color-secondary); }
.digest { font-family: monospace; font-size: 12px; }
@media (max-width: 640px) { .contract-form__grid { grid-template-columns: 1fr; } }
</style>
