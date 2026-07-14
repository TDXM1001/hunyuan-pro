<script setup lang="ts">
import type {
  BpmOperationsActionType,
  BpmOperationsCaseVO,
  BpmOperationsCaseDetailVO,
  BpmOperationsMetricVO,
} from '#/api/system/bpm';
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, onMounted, reactive, ref } from 'vue';

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
  ElCol,
  ElDescriptions,
  ElDescriptionsItem,
  ElDrawer,
  ElEmpty,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElRow,
  ElSelect,
  ElSpace,
  ElStatistic,
  ElTag,
  ElTooltip,
} from 'element-plus';

import {
  evaluateBpmOperationsRetention,
  executeBpmOperationsAction,
  exportBpmOperationsCases,
  getBpmOperationsCaseDetail,
  queryBpmOperationsCasePage,
  queryBpmOperationsMetrics,
} from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmOperationsWorkbench' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmOperationsCaseVO[]>([]);
const metrics = ref<BpmOperationsMetricVO[]>([]);
const detailVisible = ref(false);
const detail = ref<BpmOperationsCaseDetailVO>();

const searchForm = reactive({
  assigneeEmployeeId: undefined as number | undefined,
  businessKey: '',
  caseStatus: '',
  eventId: '',
  failureCode: '',
  graphDefinitionVersionId: undefined as number | undefined,
  slaLevel: '',
});

const pagination = reactive({ current: 1, size: 10, total: 0 });

const metricSummary = computed(() =>
  metrics.value.reduce(
    (summary, item) => {
      summary.averageHandlingMinutes += item.averageHandlingMinutes * item.totalCount;
      summary.openCount += item.openCount;
      summary.slaBreachedCount += item.slaBreachedCount;
      summary.totalCount += item.totalCount;
      return summary;
    },
    { averageHandlingMinutes: 0, openCount: 0, slaBreachedCount: 0, totalCount: 0 },
  ),
);

const weightedHandlingMinutes = computed(() => {
  const summary = metricSummary.value;
  return summary.totalCount
    ? Math.round(summary.averageHandlingMinutes / summary.totalCount)
    : 0;
});

const columnsFactory = (): ColumnOption<BpmOperationsCaseVO>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'caseCode', label: '工单编号', minWidth: 160 },
  { prop: 'businessKey', label: '业务键', minWidth: 170 },
  { prop: 'eventId', label: '事件ID', minWidth: 170 },
  { prop: 'nodeName', label: '节点', minWidth: 130 },
  { prop: 'caseStatus', label: '状态', width: 100, align: 'center', useSlot: true },
  { prop: 'slaLevel', label: 'SLA', width: 100, align: 'center', useSlot: true },
  { prop: 'failureCode', label: '失败码', minWidth: 150 },
  { prop: 'failureReason', label: '失败原因', minWidth: 220, showOverflowTooltip: true },
  { prop: 'assigneeEmployeeId', label: '处理人', width: 100, align: 'center' },
  { prop: 'openedAt', label: '发现时间', minWidth: 170 },
  { prop: 'actions', label: '操作', width: 250, align: 'center', fixed: 'right', useSlot: true },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);
const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() =>
  hasPagination.value ? 'calc(100% - 44px)' : '100%',
);

function caseStatusLabel(status: string) {
  return ({
    ARCHIVED: '已归档', BLOCKED: '阻塞', OPEN: '待处理', PROCESSING: '处理中',
    RESOLVED: '已恢复', TERMINATED: '已终止',
  } as Record<string, string>)[status] || status;
}

function caseStatusType(status: string) {
  if (status === 'RESOLVED' || status === 'ARCHIVED') return 'success';
  if (status === 'TERMINATED') return 'danger';
  if (status === 'BLOCKED') return 'warning';
  return 'info';
}

function slaType(level: string) {
  if (level === 'BREACHED') return 'danger';
  if (level === 'WARNING') return 'warning';
  return 'success';
}

async function loadData() {
  loading.value = true;
  try {
    const [page, metricRows] = await Promise.all([
      queryBpmOperationsCasePage({
        ...searchForm,
        pageNum: pagination.current,
        pageSize: pagination.size,
      }),
      queryBpmOperationsMetrics({
        graphDefinitionVersionId: searchForm.graphDefinitionVersionId,
      }),
    ]);
    rows.value = page?.list ?? [];
    pagination.total = page?.total ?? 0;
    metrics.value = metricRows ?? [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
  void loadData();
}

function handleReset() {
  Object.assign(searchForm, {
    assigneeEmployeeId: undefined,
    businessKey: '',
    caseStatus: '',
    eventId: '',
    failureCode: '',
    graphDefinitionVersionId: undefined,
    slaLevel: '',
  });
  pagination.current = 1;
  void loadData();
}

async function handleAction(row: BpmOperationsCaseVO, actionType: BpmOperationsActionType, label: string) {
  const { value } = await ElMessageBox.prompt(`请输入${label}原因`, `${label}运营工单`, {
    confirmButtonText: '确认',
    inputErrorMessage: '请输入处置原因',
    inputPattern: row.highRiskFlag ? /.{6,}/ : /\S+/,
    inputPlaceholder: row.highRiskFlag ? '高风险动作至少说明 6 个字符' : '说明判断依据和预期结果',
    type: actionType === 'TERMINATE' ? 'error' : 'warning',
  });
  await executeBpmOperationsAction(row.operationsCaseId, {
    actionType,
    idempotencyKey: `${actionType}:${row.operationsCaseId}:${Date.now()}`,
    reason: String(value || ''),
  });
  ElMessage.success(`${label}命令已记录`);
  await loadData();
}

async function handleRetention(row: BpmOperationsCaseVO) {
  const decision = await evaluateBpmOperationsRetention(row.operationsCaseId);
  if (decision.allowed) {
    await ElMessageBox.confirm('保留规则允许归档，是否提交归档命令？', '归档评估', {
      confirmButtonText: '归档', type: 'warning',
    });
    await handleAction(row, 'ARCHIVE', '归档');
    return;
  }
  ElMessage.warning(decision.reason);
}

async function handleDetail(row: BpmOperationsCaseVO) {
  detail.value = await getBpmOperationsCaseDetail(row.operationsCaseId);
  detailVisible.value = true;
}

function csvCell(value: unknown) {
  return `"${String(value ?? '').replaceAll('"', '""')}"`;
}

async function handleExport() {
  const exportRows = await exportBpmOperationsCases({
    ...searchForm,
    pageNum: 1,
    pageSize: 500,
  });
  const columns = ['工单编号', '业务键', '事件ID', '定义版本', '节点', '状态', 'SLA', '失败码', '失败原因', '处理人', '发现时间'];
  const lines = (exportRows ?? []).map((row) => [
    row.caseCode, row.businessKey, row.eventId, row.graphDefinitionVersionId,
    row.nodeName, row.caseStatus, row.slaLevel, row.failureCode,
    row.failureReason, row.assigneeEmployeeId, row.openedAt,
  ].map(csvCell).join(','));
  const blob = new Blob([`\uFEFF${columns.join(',')}\n${lines.join('\n')}`], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `bpm-operations-${Date.now()}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

function handleCurrentChange(value: number) {
  pagination.current = value;
  void loadData();
}

function handleSizeChange(value: number) {
  pagination.size = value;
  pagination.current = 1;
  void loadData();
}

onMounted(() => void loadData().catch((error) => {
  ElMessage.error(error?.message || '运营治理数据加载失败');
}));
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="operations-page">
      <ElCard v-show="showSearchBar" class="operations-page__search" shadow="never">
        <ArtSearchPanel :collapsible="true" :loading="loading" :show-refresh="false" @reset="handleReset" @search="handleSearch">
          <ElFormItem label="业务键"><ElInput v-model="searchForm.businessKey" clearable placeholder="请输入业务键" /></ElFormItem>
          <ElFormItem label="定义版本"><ElInputNumber v-model="searchForm.graphDefinitionVersionId" :controls="false" :min="1" placeholder="版本ID" /></ElFormItem>
          <ElFormItem label="处理人"><ElInputNumber v-model="searchForm.assigneeEmployeeId" :controls="false" :min="1" placeholder="员工ID" /></ElFormItem>
          <ElFormItem label="状态"><ElSelect v-model="searchForm.caseStatus" clearable placeholder="请选择状态"><ElOption label="待处理" value="OPEN" /><ElOption label="处理中" value="PROCESSING" /><ElOption label="阻塞" value="BLOCKED" /><ElOption label="已恢复" value="RESOLVED" /><ElOption label="已终止" value="TERMINATED" /></ElSelect></ElFormItem>
          <ElFormItem label="SLA"><ElSelect v-model="searchForm.slaLevel" clearable placeholder="请选择 SLA"><ElOption label="正常" value="NORMAL" /><ElOption label="预警" value="WARNING" /><ElOption label="超时" value="BREACHED" /></ElSelect></ElFormItem>
          <ElFormItem label="失败码"><ElInput v-model="searchForm.failureCode" clearable placeholder="请输入失败码" /></ElFormItem>
          <ElFormItem label="事件ID"><ElInput v-model="searchForm.eventId" clearable placeholder="请输入事件ID" /></ElFormItem>
        </ArtSearchPanel>
      </ElCard>

      <ElRow :gutter="12" class="operations-page__metrics">
        <ElCol :span="6"><ElStatistic title="异常总量" :value="metricSummary.totalCount" /></ElCol>
        <ElCol :span="6"><ElStatistic title="当前积压" :value="metricSummary.openCount" /></ElCol>
        <ElCol :span="6"><ElStatistic title="SLA 超时" :value="metricSummary.slaBreachedCount" /></ElCol>
        <ElCol :span="6"><ElStatistic title="平均处理时长" :value="weightedHandlingMinutes" suffix=" 分钟" /></ElCol>
      </ElRow>

      <ElCard class="operations-page__table" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader v-model="columnChecks" :loading="loading" layout="search,size,fullscreen,columns,settings" :show-search-bar="showSearchBar" @search="showSearchBar = !showSearchBar">
            <template #left><ElButton type="primary" plain @click="handleExport">导出</ElButton></template>
          </ArtTableHeader>
          <ArtTable :columns="columns" :data="rows" :height="tableHeight" :loading="loading" :pagination="pagination" :pagination-options="{ align: 'center', hideOnSinglePage: false, layout: 'sizes, prev, pager, next, jumper', pageSizes: [10, 20, 30], showTotalSummary: true, size: 'small' }" @pagination:current-change="handleCurrentChange" @pagination:size-change="handleSizeChange">
            <template #caseStatus="{ row }"><ElTag :type="caseStatusType(row.caseStatus)" effect="plain" size="small">{{ caseStatusLabel(row.caseStatus) }}</ElTag></template>
            <template #slaLevel="{ row }"><ElTag :type="slaType(row.slaLevel)" effect="plain" size="small">{{ row.slaLevel }}</ElTag></template>
            <template #actions="{ row }">
              <ElSpace class="operations-page__actions">
                <ElButton link type="primary" @click="handleDetail(row)">审计</ElButton>
                <ElButton v-if="row.retryableFlag" link type="primary" @click="handleAction(row, 'RETRY', '重试')">重试</ElButton>
                <ElButton v-if="row.compensableFlag" link type="warning" @click="handleAction(row, 'COMPENSATE', '补偿')">补偿</ElButton>
                <ElTooltip content="终止会结束当前异常工单" placement="top"><ElButton v-if="['OPEN', 'PROCESSING', 'BLOCKED'].includes(row.caseStatus)" link type="danger" @click="handleAction(row, 'TERMINATE', '终止')">终止</ElButton></ElTooltip>
                <ElButton link type="info" @click="handleRetention(row)">归档评估</ElButton>
              </ElSpace>
            </template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>

      <ElDrawer v-model="detailVisible" size="560px" title="处置审计">
        <template v-if="detail">
          <ElDescriptions :column="2" border>
            <ElDescriptionsItem label="工单编号">{{ detail.caseCode }}</ElDescriptionsItem>
            <ElDescriptionsItem label="当前状态">{{ caseStatusLabel(detail.caseStatus) }}</ElDescriptionsItem>
            <ElDescriptionsItem label="业务键">{{ detail.businessKey || '-' }}</ElDescriptionsItem>
            <ElDescriptionsItem label="失败码">{{ detail.failureCode || '-' }}</ElDescriptionsItem>
          </ElDescriptions>
          <div v-if="detail.actionLogs.length" class="operations-page__audit-list">
            <div v-for="item in detail.actionLogs" :key="item.operationsActionLogId" class="operations-page__audit-item">
              <div class="operations-page__audit-title">
                <ElTag :type="item.actionStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ item.actionStatus }}</ElTag>
                <strong>{{ item.actionType }}</strong>
                <span>{{ item.actionAt }}</span>
              </div>
              <div>操作人：{{ item.actorEmployeeId || '-' }}</div>
              <div>原因：{{ item.reason }}</div>
              <div v-if="item.failureReason">失败：{{ item.failureReason }}</div>
            </div>
          </div>
          <ElEmpty v-else description="暂无处置记录" />
        </template>
      </ElDrawer>
    </div>
  </Page>
</template>

<style scoped>
.operations-page { display: flex; flex-direction: column; gap: 12px; height: 100%; min-height: 0; overflow: hidden; }
.operations-page__search { border: 0; border-radius: 8px; flex-shrink: 0; }
.operations-page__search :deep(.el-card__body) { padding: 16px; }
.operations-page__metrics { flex-shrink: 0; margin: 0 !important; padding: 10px 16px; border: 1px solid var(--el-border-color-lighter); background: var(--el-bg-color); }
.operations-page__metrics :deep(.el-col:not(:first-child)) { border-left: 1px solid var(--el-border-color-lighter); padding-left: 24px !important; }
.operations-page__table { border: 1px solid var(--el-border-color-lighter); border-radius: 8px; flex: 1; min-height: 0; overflow: hidden; }
.operations-page__table :deep(.el-card__body) { display: flex; flex-direction: column; height: 100%; min-height: 0; overflow: hidden; padding: 16px; }
.operations-page :deep(.art-table-panel), .operations-page :deep(.art-table) { flex: 1; min-height: 0; }
.operations-page :deep(.art-table-header) { margin-bottom: 18px; }
.operations-page__actions { display: inline-flex; gap: 8px; justify-content: center; }
.operations-page__actions :deep(.el-button) { margin-left: 0; padding: 0; }
.operations-page__audit-list { display: flex; flex-direction: column; gap: 12px; margin-top: 16px; }
.operations-page__audit-item { border-bottom: 1px solid var(--el-border-color-lighter); display: grid; gap: 6px; padding-bottom: 12px; }
.operations-page__audit-title { align-items: center; display: flex; gap: 8px; }
.operations-page__audit-title span:last-child { color: var(--el-text-color-secondary); margin-left: auto; }
@media (max-width: 768px) { .operations-page__metrics { row-gap: 12px; } .operations-page__metrics :deep(.el-col) { max-width: 50%; flex: 0 0 50%; } }
</style>
