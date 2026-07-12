<script setup lang="ts">
import type {
  BpmPolicyCatalogRecord,
  BpmPolicyType,
  CreateBpmPolicyDraftParams,
} from '#/api/system/bpm';
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, onMounted, reactive, ref, watch } from 'vue';

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
  activateBpmPolicyVersion,
  copyBpmPolicyAsDraft,
  createBpmPolicyDraft,
  queryBpmPolicyCatalog,
  retireBpmPolicyVersion,
  validateBpmPolicyDraft,
} from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmPolicyCatalog' });

const loading = ref(false);
const saving = ref(false);
const showSearchBar = ref(true);
const draftVisible = ref(false);
const detailVisible = ref(false);
const validationPayload = ref<string>();
const selected = ref<BpmPolicyCatalogRecord>();
const rows = ref<BpmPolicyCatalogRecord[]>([]);
const filter = reactive<{
  lifecycleState: '' | BpmPolicyCatalogRecord['lifecycleState'];
  policyKey: string;
  type: BpmPolicyType;
}>({ lifecycleState: '', policyKey: '', type: 'CANDIDATE' });

const policyTemplates: Record<BpmPolicyType, string> = {
  APPROVAL: JSON.stringify({
    completionMode: 'ALL',
    ratioPercent: 100,
    rejectionRule: 'IMMEDIATE',
    allowedActions: ['APPROVE', 'REJECT', 'RETURN'],
    returnRule: 'RETURN_INITIATOR',
    terminationRule: 'CANCEL_REMAINING_MEMBERS',
    riskLevel: 'LOW',
  }, null, 2),
  CANDIDATE: JSON.stringify({
    resolverType: 'ROLE',
    resolverParameters: { roleId: 1 },
    resolutionPhase: 'ACTIVATE',
    memberOrder: 'EMPLOYEE_ID',
    duplicateRule: 'SOURCE_EMPLOYEE',
    emptyCandidatePolicy: 'BLOCK',
    selfApprovalPolicy: 'BLOCK',
    riskLevel: 'LOW',
  }, null, 2),
  START_VISIBILITY: JSON.stringify({
    startScope: { type: 'EMPLOYEE_IDS', employeeIds: [1] },
    visibilityScope: { type: 'EMPLOYEE_IDS', employeeIds: [1] },
    riskLevel: 'LOW',
  }, null, 2),
};

const createDraft = (type: BpmPolicyType = filter.type): CreateBpmPolicyDraftParams => ({
  policyJson: policyTemplates[type],
  policyKey: '',
  schemaVersion: 1,
  type,
});
const draft = reactive<CreateBpmPolicyDraftParams>(createDraft());

const columnsFactory = (): ColumnOption<BpmPolicyCatalogRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 68, align: 'center' },
  { prop: 'policyKey', label: '策略编码', minWidth: 180, useSlot: true },
  { prop: 'type', label: '类型', width: 150, useSlot: true },
  { prop: 'version', label: '版本', width: 86, align: 'center', useSlot: true },
  { prop: 'lifecycleState', label: '状态', width: 112, useSlot: true },
  { prop: 'summary', label: '策略摘要', minWidth: 300, useSlot: true },
  { prop: 'operation', label: '操作', width: 260, fixed: 'right', useSlot: true },
];
const { columns, columnChecks } = useTableColumns(columnsFactory);
const tableHeight = computed(() => '100%');

function typeLabel(type: BpmPolicyType) {
  return type === 'CANDIDATE' ? '候选策略' : type === 'APPROVAL' ? '审批策略' : '发起可见范围';
}

function stateType(state: BpmPolicyCatalogRecord['lifecycleState']) {
  return state === 'ACTIVE' ? 'success' : state === 'DRAFT' ? 'warning' : 'info';
}

function summary(record: BpmPolicyCatalogRecord) {
  try {
    const payload = JSON.parse(record.canonicalPayload) as Record<string, unknown>;
    if (record.reference.type === 'CANDIDATE') {
      return `解析器 ${String(payload.resolverType || '-')}, 时机 ${String(payload.resolutionPhase || '-')}`;
    }
    if (record.reference.type === 'APPROVAL') {
      return `${String(payload.completionMode || '-')}，拒绝规则 ${String(payload.rejectionRule || '-')}`;
    }
    return `发起 ${String((payload.startScope as Record<string, unknown> | undefined)?.type || '-')}, 可见 ${(payload.visibilityScope as Record<string, unknown> | undefined)?.type || '-'}`;
  } catch {
    return '策略内容不可解析';
  }
}

async function loadData() {
  loading.value = true;
  try {
    rows.value = await queryBpmPolicyCatalog({
      lifecycleState: filter.lifecycleState || undefined,
      policyKey: filter.policyKey.trim() || undefined,
      type: filter.type,
    }) || [];
  } finally {
    loading.value = false;
  }
}

function search() {
  void loadData();
}

function reset() {
  Object.assign(filter, { lifecycleState: '', policyKey: '', type: 'CANDIDATE' as BpmPolicyType });
  void loadData();
}

function openDraft() {
  Object.assign(draft, createDraft(filter.type));
  validationPayload.value = undefined;
  draftVisible.value = true;
}

watch(() => draft.type, (type) => {
  draft.policyJson = policyTemplates[type];
  validationPayload.value = undefined;
});

async function validateDraft() {
  if (!draft.policyKey.trim() || !draft.policyJson.trim()) {
    ElMessage.warning('请填写策略编码和策略内容');
    return;
  }
  const result = await validateBpmPolicyDraft(draft);
  validationPayload.value = result.canonicalPayload;
  ElMessage.success('策略结构校验通过');
}

async function submitDraft() {
  if (!draft.policyKey.trim() || !draft.policyJson.trim()) {
    ElMessage.warning('请填写策略编码和策略内容');
    return;
  }
  saving.value = true;
  try {
    await createBpmPolicyDraft({ ...draft, policyKey: draft.policyKey.trim() });
    ElMessage.success('已创建新的策略草稿版本');
    draftVisible.value = false;
    await loadData();
  } finally {
    saving.value = false;
  }
}

async function copyAsDraft(record: BpmPolicyCatalogRecord) {
  await copyBpmPolicyAsDraft(record.reference);
  ElMessage.success('已复制为新的策略草稿版本');
  await loadData();
}

async function activate(record: BpmPolicyCatalogRecord) {
  await ElMessageBox.confirm('启用后内容不可原地编辑，是否继续？', '启用策略版本', { type: 'warning' });
  await activateBpmPolicyVersion({ ...record.reference, catalogRevision: record.catalogRevision });
  ElMessage.success('策略版本已启用');
  await loadData();
}

async function retire(record: BpmPolicyCatalogRecord) {
  await ElMessageBox.confirm('退休后新流程不能再引用该版本，已发布定义仍按冻结快照运行。是否继续？', '退休策略版本', { type: 'warning' });
  await retireBpmPolicyVersion({ ...record.reference, catalogRevision: record.catalogRevision });
  ElMessage.success('策略版本已退休');
  await loadData();
}

function openDetail(record: BpmPolicyCatalogRecord) {
  selected.value = record;
  detailVisible.value = true;
}

onMounted(() => void loadData());
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="policy-page">
      <ElCard v-show="showSearchBar" shadow="never">
        <ArtSearchPanel :collapsible="false" :loading="loading" :show-refresh="false" @reset="reset" @search="search">
          <ElFormItem label="策略类型">
            <ElSelect v-model="filter.type">
              <ElOption label="候选策略" value="CANDIDATE" />
              <ElOption label="审批策略" value="APPROVAL" />
              <ElOption label="发起可见范围" value="START_VISIBILITY" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="策略编码"><ElInput v-model="filter.policyKey" clearable /></ElFormItem>
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
            <template #left><ElButton type="primary" @click="openDraft">新增策略草稿</ElButton></template>
          </ArtTableHeader>
          <ArtTable :columns="columns" :data="rows" :height="tableHeight" :loading="loading">
            <template #policyKey="{ row }"><span>{{ row.reference.policyKey }}</span></template>
            <template #type="{ row }"><ElTag effect="plain">{{ typeLabel(row.reference.type) }}</ElTag></template>
            <template #version="{ row }">v{{ row.reference.policyVersion }}</template>
            <template #lifecycleState="{ row }"><ElTag :type="stateType(row.lifecycleState)" effect="plain">{{ row.lifecycleState }}</ElTag></template>
            <template #summary="{ row }">{{ summary(row) }}</template>
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

    <ElDialog v-model="draftVisible" destroy-on-close title="新建策略草稿" width="min(760px, 96vw)">
      <ElForm label-position="top" class="policy-form">
        <div class="policy-form__grid">
          <ElFormItem label="策略类型">
            <ElSelect v-model="draft.type">
              <ElOption label="候选策略" value="CANDIDATE" />
              <ElOption label="审批策略" value="APPROVAL" />
              <ElOption label="发起可见范围" value="START_VISIBILITY" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="策略编码"><ElInput v-model="draft.policyKey" /></ElFormItem>
        </div>
        <ElFormItem label="策略 JSON"><ElInput v-model="draft.policyJson" :rows="14" type="textarea" /></ElFormItem>
        <ElFormItem v-if="validationPayload" label="规范化结果"><ElInput :model-value="validationPayload" :rows="8" readonly type="textarea" /></ElFormItem>
        <p class="policy-form__hint">已启用版本只可复制为新草稿；策略内容不接受表达式、脚本或自由 SQL。</p>
      </ElForm>
      <template #footer>
        <ElButton @click="draftVisible = false">取消</ElButton>
        <ElButton @click="validateDraft">校验</ElButton>
        <ElButton :loading="saving" type="primary" @click="submitDraft">创建草稿</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="detailVisible" title="策略版本详情" width="min(840px, 96vw)">
      <template v-if="selected">
        <div class="policy-detail__meta">
          <ElTag>{{ typeLabel(selected.reference.type) }}</ElTag>
          <ElTag effect="plain">{{ selected.reference.policyKey }} v{{ selected.reference.policyVersion }}</ElTag>
          <ElTag :type="stateType(selected.lifecycleState)" effect="plain">{{ selected.lifecycleState }}</ElTag>
          <span>revision {{ selected.catalogRevision }}</span>
        </div>
        <ElInput :model-value="selected.canonicalPayload" :rows="18" readonly type="textarea" />
      </template>
      <template #footer><ElButton @click="detailVisible = false">关闭</ElButton></template>
    </ElDialog>
  </Page>
</template>

<style scoped>
.policy-page { display: flex; flex-direction: column; gap: 12px; height: 100%; min-height: 0; }
.policy-page > .el-card:first-child { border: 0; flex-shrink: 0; }
.table-card { flex: 1; min-height: 0; overflow: hidden; }
.table-card :deep(.el-card__body) { display: flex; flex-direction: column; height: 100%; min-height: 0; padding: 16px; }
.policy-form__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }
.policy-form :deep(.el-select) { width: 100%; }
.policy-form__hint { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }
.policy-detail__meta { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 12px; color: var(--el-text-color-secondary); font-size: 13px; }
@media (max-width: 640px) { .policy-form__grid { grid-template-columns: 1fr; } }
</style>
