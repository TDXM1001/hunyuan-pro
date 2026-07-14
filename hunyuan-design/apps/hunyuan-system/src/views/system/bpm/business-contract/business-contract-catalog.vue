<script setup lang="ts">
import type { BpmBusinessContractLifecycleState, BpmBusinessObjectSummary } from '#/api/system/bpm';
import type { ColumnOption } from '@vben/art-hooks/table';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ArtSearchPanel } from '@vben/art-hooks/common';
import { ArtTable, ArtTableHeader, ArtTablePanel, useTableColumns } from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';
import { ElButton, ElCard, ElFormItem, ElInput, ElMessage, ElMessageBox, ElOption, ElSelect, ElTag } from 'element-plus';
import { activateBpmBusinessContract, copyBpmBusinessContractAsDraft, deleteBpmBusinessObjectDraft, queryBpmBusinessContracts, retireBpmBusinessContract } from '#/api/system/bpm';

const router = useRouter();
const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmBusinessObjectSummary[]>([]);
const filter = reactive<{ contractKey: string; lifecycleState: '' | BpmBusinessContractLifecycleState }>({ contractKey: '', lifecycleState: '' });
const columnsFactory = (): ColumnOption<BpmBusinessObjectSummary>[] => [
  { type: 'globalIndex', label: '序号', width: 68, align: 'center' },
  { prop: 'objectName', label: '业务对象', minWidth: 150 },
  { prop: 'contractKey', label: '编码', minWidth: 150 },
  { prop: 'contractVersion', label: '版本', width: 76, useSlot: true },
  { prop: 'lifecycleState', label: '状态', width: 100, useSlot: true },
  { prop: 'businessSummary', label: '业务摘要', minWidth: 320 },
  { prop: 'referenceCount', label: '引用', width: 70, align: 'center' },
  { prop: 'operation', label: '操作', width: 280, fixed: 'right', useSlot: true },
];
const { columns, columnChecks } = useTableColumns(columnsFactory);
const tableHeight = computed(() => '100%');

async function loadData() {
  loading.value = true;
  try {
    rows.value = await queryBpmBusinessContracts({ contractKey: filter.contractKey.trim() || undefined, lifecycleState: filter.lifecycleState || undefined }) || [];
  } finally { loading.value = false; }
}
function reset() { Object.assign(filter, { contractKey: '', lifecycleState: '' }); void loadData(); }
function routeQuery(row: BpmBusinessObjectSummary) { return { contractKey: row.contractKey, contractVersion: String(row.contractVersion) }; }
function createObject() { void router.push('/system/bpm/business-contract/editor'); }
function detail(row: BpmBusinessObjectSummary) { void router.push({ path: '/system/bpm/business-contract/detail', query: routeQuery(row) }); }
function edit(row: BpmBusinessObjectSummary) { void router.push({ path: '/system/bpm/business-contract/editor', query: routeQuery(row) }); }
async function copy(row: BpmBusinessObjectSummary) { const draft = await copyBpmBusinessContractAsDraft(row); ElMessage.success('已复制为新草稿'); await loadData(); edit({ ...row, ...draft } as BpmBusinessObjectSummary); }
async function remove(row: BpmBusinessObjectSummary) { await ElMessageBox.confirm('删除后无法恢复，确认删除这个未引用草稿？', '删除草稿', { type: 'warning' }); await deleteBpmBusinessObjectDraft(row); await loadData(); }
async function activate(row: BpmBusinessObjectSummary) { await ElMessageBox.confirm('启用后该版本只读，确认继续？', '启用业务对象'); await activateBpmBusinessContract(row); await loadData(); }
async function retire(row: BpmBusinessObjectSummary) { await ElMessageBox.confirm('退休后新 Graph 不能引用，既有实例不受影响。', '退休业务对象', { type: 'warning' }); await retireBpmBusinessContract(row); await loadData(); }
onMounted(() => void loadData());
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="object-page">
      <ElCard v-show="showSearchBar" shadow="never">
        <ArtSearchPanel :collapsible="false" :loading="loading" :show-refresh="false" @reset="reset" @search="loadData">
          <ElFormItem label="名称或编码"><ElInput v-model="filter.contractKey" clearable /></ElFormItem>
          <ElFormItem label="状态"><ElSelect v-model="filter.lifecycleState" clearable><ElOption label="草稿" value="DRAFT" /><ElOption label="已启用" value="ACTIVE" /><ElOption label="已退休" value="RETIRED" /></ElSelect></ElFormItem>
        </ArtSearchPanel>
      </ElCard>
      <ElCard class="table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader v-model="columnChecks" :loading="loading" layout="search,size,fullscreen,columns,settings" :show-search-bar="showSearchBar" @search="showSearchBar = !showSearchBar"><template #left><ElButton type="primary" @click="createObject">新增业务对象</ElButton></template></ArtTableHeader>
          <ArtTable :columns="columns" :data="rows" :height="tableHeight" :loading="loading">
            <template #contractVersion="{ row }">v{{ row.contractVersion }}</template>
            <template #lifecycleState="{ row }"><ElTag :type="row.lifecycleState === 'ACTIVE' ? 'success' : row.lifecycleState === 'DRAFT' ? 'warning' : 'info'">{{ row.lifecycleState === 'ACTIVE' ? '已启用' : row.lifecycleState === 'DRAFT' ? '草稿' : '已退休' }}</ElTag></template>
            <template #operation="{ row }"><span class="object-actions"><ElButton link type="primary" @click="detail(row)">详情</ElButton><ElButton v-if="row.lifecycleState === 'DRAFT'" link type="primary" @click="edit(row)">继续编辑</ElButton><ElButton link @click="copy(row)">复制</ElButton><ElButton v-if="row.lifecycleState === 'DRAFT'" link type="success" @click="activate(row)">启用</ElButton><ElButton v-if="row.lifecycleState === 'DRAFT'" link type="danger" @click="remove(row)">删除</ElButton><ElButton v-if="row.lifecycleState === 'ACTIVE'" link type="danger" @click="retire(row)">退休</ElButton></span></template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.object-page { display: flex; flex-direction: column; gap: 12px; height: 100%; min-height: 0; }
.object-page > .el-card:first-child { border: 0; flex-shrink: 0; }
.table-card { flex: 1; min-height: 0; overflow: hidden; }
.table-card :deep(.el-card__body) { display: flex; flex-direction: column; height: 100%; min-height: 0; padding: 16px; }
.object-actions { display: inline-flex; align-items: center; justify-content: center; gap: 8px; }
.object-actions :deep(.el-button) { padding: 0; margin-left: 0; }
</style>
