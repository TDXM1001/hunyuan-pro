<script setup lang="ts">
import type { BpmPolicyCatalogRecord, BpmPolicyType } from '#/api/system/bpm';
import type { DictOption } from '#/api/system/dict';
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import {
  ArtTable,
  ArtTableHeader,
  ArtTablePanel,
  useTableColumns,
} from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElCard,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTag,
} from 'element-plus';

import {
  copyBpmPolicyAsDraft,
  deleteBpmPolicyDraft,
  queryBpmPolicyCatalog,
  retireBpmPolicyVersion,
} from '#/api/system/bpm';
import { queryDictOptionsByCode } from '#/api/system/dict';

defineOptions({ name: 'SystemBpmPolicyCatalog' });

const router = useRouter();
const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmPolicyCatalogRecord[]>([]);
const typeOptions = ref<DictOption[]>([]);
const lifecycleOptions = ref<DictOption[]>([]);

const filter = reactive<{
  lifecycleState: '' | BpmPolicyCatalogRecord['lifecycleState'];
  policyKey: string;
  type: BpmPolicyType;
}>({
  lifecycleState: '',
  policyKey: '',
  type: 'CANDIDATE',
});

const columnsFactory = (): ColumnOption<BpmPolicyCatalogRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 68, align: 'center' },
  { prop: 'policyName', label: '规则名称', minWidth: 160 },
  { prop: 'type', label: '类型', width: 130, useSlot: true },
  { prop: 'version', label: '版本', width: 80, useSlot: true },
  { prop: 'lifecycleState', label: '状态', width: 100, useSlot: true },
  { prop: 'calculatedRiskLevel', label: '风险', width: 90, useSlot: true },
  { prop: 'businessSummary', label: '业务摘要', minWidth: 320 },
  { prop: 'referenceCount', label: '引用', width: 76, align: 'center' },
  { prop: 'operation', label: '操作', width: 260, fixed: 'right', useSlot: true },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);
const tableHeight = computed(() => '100%');

const typeLabelMap = computed(() =>
  Object.fromEntries(typeOptions.value.map((item) => [item.value, item.label])),
);

const stateLabelMap = computed(() =>
  Object.fromEntries(
    lifecycleOptions.value.map((item) => [item.value, item.label]),
  ),
);

function typeLabel(type: BpmPolicyType) {
  return typeLabelMap.value[type] || type;
}

function stateLabel(state: string) {
  return stateLabelMap.value[state] || state;
}

async function loadDictOptions() {
  try {
    const [types, lifecycles] = await Promise.all([
      queryDictOptionsByCode('BPM_POLICY_TYPE'),
      queryDictOptionsByCode('BPM_POLICY_LIFECYCLE_STATE'),
    ]);
    typeOptions.value = types;
    lifecycleOptions.value = lifecycles;
  } catch (error: any) {
    ElMessage.error(error?.message || '审批规则字典加载失败');
  }
}

async function loadData() {
  loading.value = true;
  try {
    rows.value =
      (await queryBpmPolicyCatalog({
        type: filter.type,
        lifecycleState: filter.lifecycleState || undefined,
        policyKey: filter.policyKey.trim() || undefined,
      })) || [];
  } finally {
    loading.value = false;
  }
}

function reset() {
  Object.assign(filter, {
    lifecycleState: '',
    policyKey: '',
    type: 'CANDIDATE',
  });
  void loadData();
}

function createRule() {
  void router.push({
    path: '/system/bpm/policy/editor',
    query: { type: filter.type },
  });
}

function referenceQuery(row: BpmPolicyCatalogRecord) {
  return {
    type: row.reference.type,
    policyKey: row.reference.policyKey,
    policyVersion: String(row.reference.policyVersion),
  };
}

function openDetail(row: BpmPolicyCatalogRecord) {
  void router.push({
    path: '/system/bpm/policy/detail',
    query: referenceQuery(row),
  });
}

function continueEdit(row: BpmPolicyCatalogRecord) {
  void router.push({
    path: '/system/bpm/policy/editor',
    query: referenceQuery(row),
  });
}

async function copyRule(row: BpmPolicyCatalogRecord) {
  const draft = await copyBpmPolicyAsDraft(row.reference);
  ElMessage.success('已复制为新草稿');
  await loadData();
  if (draft?.reference) {
    continueEdit(draft);
  }
}

async function deleteDraft(row: BpmPolicyCatalogRecord) {
  await ElMessageBox.confirm(
    '删除后无法恢复，确认删除这个未引用草稿？',
    '删除草稿',
    { type: 'warning' },
  );
  await deleteBpmPolicyDraft({
    ...row.reference,
    catalogRevision: row.catalogRevision,
  });
  ElMessage.success('草稿已删除');
  await loadData();
}

async function retire(row: BpmPolicyCatalogRecord) {
  await ElMessageBox.confirm(
    '退休后新流程不能再引用，已发布流程继续使用冻结版本。',
    '退休规则',
    { type: 'warning' },
  );
  await retireBpmPolicyVersion({
    ...row.reference,
    catalogRevision: row.catalogRevision,
  });
  await loadData();
}

onMounted(() => {
  void Promise.all([loadDictOptions(), loadData()]);
});
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="policy-page">
      <ElCard v-show="showSearchBar" shadow="never">
        <ArtSearchPanel
          :collapsible="false"
          :loading="loading"
          :show-refresh="false"
          @reset="reset"
          @search="loadData"
        >
          <ElFormItem label="规则类型">
            <ElSelect
              v-model="filter.type"
              no-data-text="请先在字典中心维护 BPM_POLICY_TYPE"
            >
              <ElOption
                v-for="item in typeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="名称或编码">
            <ElInput v-model="filter.policyKey" clearable />
          </ElFormItem>
          <ElFormItem label="状态">
            <ElSelect
              v-model="filter.lifecycleState"
              clearable
              no-data-text="请先在字典中心维护 BPM_POLICY_LIFECYCLE_STATE"
            >
              <ElOption
                v-for="item in lifecycleOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElCard class="table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader
            v-model="columnChecks"
            :loading="loading"
            layout="search,size,fullscreen,columns,settings"
            :show-search-bar="showSearchBar"
            @search="showSearchBar = !showSearchBar"
          >
            <template #left>
              <ElButton type="primary" @click="createRule">
                新增审批规则
              </ElButton>
            </template>
          </ArtTableHeader>

          <ArtTable
            :columns="columns"
            :data="rows"
            :height="tableHeight"
            :loading="loading"
          >
            <template #type="{ row }">
              <ElTag effect="plain">
                {{ typeLabel(row.reference.type) }}
              </ElTag>
            </template>
            <template #version="{ row }">
              v{{ row.reference.policyVersion }}
            </template>
            <template #lifecycleState="{ row }">
              <ElTag
                :type="
                  row.lifecycleState === 'ACTIVE'
                    ? 'success'
                    : row.lifecycleState === 'DRAFT'
                      ? 'warning'
                      : 'info'
                "
              >
                {{ stateLabel(row.lifecycleState) }}
              </ElTag>
            </template>
            <template #calculatedRiskLevel="{ row }">
              <ElTag
                :type="
                  row.calculatedRiskLevel === 'HIGH'
                    ? 'danger'
                    : row.calculatedRiskLevel === 'MEDIUM'
                      ? 'warning'
                      : 'success'
                "
              >
                {{ row.calculatedRiskLevel }}
              </ElTag>
            </template>
            <template #operation="{ row }">
              <ElButton link type="primary" @click="openDetail(row)">
                详情
              </ElButton>
              <ElButton
                v-if="row.lifecycleState === 'DRAFT'"
                link
                type="primary"
                @click="continueEdit(row)"
              >
                继续编辑
              </ElButton>
              <ElButton link @click="copyRule(row)">复制</ElButton>
              <ElButton
                v-if="row.lifecycleState === 'DRAFT'"
                link
                type="danger"
                @click="deleteDraft(row)"
              >
                删除草稿
              </ElButton>
              <ElButton
                v-if="row.lifecycleState === 'ACTIVE'"
                link
                type="danger"
                @click="retire(row)"
              >
                退休
              </ElButton>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.policy-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.policy-page > .el-card:first-child {
  border: 0;
  flex-shrink: 0;
}

.table-card {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.table-card:deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 16px;
}
</style>
