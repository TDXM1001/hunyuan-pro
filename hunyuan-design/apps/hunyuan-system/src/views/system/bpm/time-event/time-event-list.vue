<script setup lang="ts">
import type { BpmTimeEventRecord } from '#/api/system/bpm';
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, onMounted, reactive, ref } from 'vue';
import { ArtSearchPanel } from '@vben/art-hooks/common';
import { ArtTable, ArtTableHeader, ArtTablePanel, useTableColumns } from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';
import { ElButton, ElCard, ElFormItem, ElInput, ElMessage, ElMessageBox, ElOption, ElSelect, ElTag } from 'element-plus';
import { queryBpmTimeEventPage, retryBpmTimeEvent } from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmTimeEventList' });
const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmTimeEventRecord[]>([]);
const searchForm = reactive({ eventKind: '', eventStatus: '', instanceId: '' });
const pagination = reactive({ current: 1, size: 10, total: 0 });
const columnsFactory = (): ColumnOption<BpmTimeEventRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'eventKind', label: '事件类型', width: 150 },
  { prop: 'eventStatus', label: '状态', width: 140, useSlot: true },
  { prop: 'instanceId', label: '实例ID', width: 110 },
  { prop: 'nodeKey', label: '节点', minWidth: 150 },
  { prop: 'scheduledAt', label: '计划时间', minWidth: 175 },
  { prop: 'triggeredAt', label: '触发时间', minWidth: 175 },
  { prop: 'triggerCount', label: '次数', width: 80, align: 'center' },
  { prop: 'lastError', label: '最后错误', minWidth: 220 },
  { prop: 'operation', label: '操作', width: 90, fixed: 'right', useSlot: true },
];
const { columns, columnChecks } = useTableColumns(columnsFactory);
const tableHeight = computed(() => pagination.total > pagination.size ? 'calc(100% - 44px)' : '100%');
function statusType(status: string) {
  if (status === 'SUCCEEDED') return 'success';
  if (status === 'FAILED_MANUAL' || status === 'FAILED_RETRYABLE') return 'danger';
  if (status === 'CANCELLED') return 'info';
  return 'warning';
}
async function loadData() {
  loading.value = true;
  try {
    const result = await queryBpmTimeEventPage({
      eventKind: searchForm.eventKind || undefined,
      eventStatus: searchForm.eventStatus || undefined,
      instanceId: searchForm.instanceId ? Number(searchForm.instanceId) : undefined,
      pageNum: pagination.current,
      pageSize: pagination.size,
    });
    rows.value = result?.list ?? [];
    pagination.total = result?.total ?? 0;
  } finally { loading.value = false; }
}
function search() { pagination.current = 1; void loadData(); }
function reset() { Object.assign(searchForm, { eventKind: '', eventStatus: '', instanceId: '' }); search(); }
async function retry(row: BpmTimeEventRecord) {
  await ElMessageBox.confirm('确认重新执行该时间事件？', '人工重试', { type: 'warning' });
  await retryBpmTimeEvent(row.timeEventId);
  ElMessage.success('重试命令已执行');
  await loadData();
}
onMounted(() => void loadData());
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden">
    <div class="ops-page">
      <ElCard v-show="showSearchBar" shadow="never">
        <ArtSearchPanel :collapsible="false" :loading="loading" :show-refresh="false" @reset="reset" @search="search">
          <ElFormItem label="实例ID"><ElInput v-model="searchForm.instanceId" clearable /></ElFormItem>
          <ElFormItem label="事件类型">
            <ElSelect v-model="searchForm.eventKind" clearable>
              <ElOption label="SLA提醒" value="SLA_REMINDER" /><ElOption label="SLA到期" value="SLA_DUE" />
              <ElOption label="延迟节点" value="DELAY" /><ElOption label="外部等待超时" value="EXTERNAL_TIMEOUT" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="状态">
            <ElSelect v-model="searchForm.eventStatus" clearable>
              <ElOption v-for="item in ['SCHEDULED','TRIGGERED','SUCCEEDED','FAILED_RETRYABLE','FAILED_MANUAL','CANCELLED']" :key="item" :label="item" :value="item" />
            </ElSelect>
          </ElFormItem>
        </ArtSearchPanel>
      </ElCard>
      <ElCard class="table-card" shadow="never">
        <ArtTablePanel>
          <ArtTableHeader v-model="columnChecks" :loading="loading" layout="search,size,fullscreen,columns,settings" :show-search-bar="showSearchBar" @search="showSearchBar = !showSearchBar" />
          <ArtTable :columns="columns" :data="rows" :height="tableHeight" :loading="loading" :pagination="pagination"
            :pagination-options="{ align:'center', hideOnSinglePage:false, layout:'sizes, prev, pager, next, jumper', pageSizes:[10,20,30], showTotalSummary:true, size:'small' }"
            @pagination:current-change="(value:number) => { pagination.current=value; void loadData(); }"
            @pagination:size-change="(value:number) => { pagination.size=value; pagination.current=1; void loadData(); }">
            <template #eventStatus="{ row }"><ElTag :type="statusType(row.eventStatus)" effect="plain">{{ row.eventStatus }}</ElTag></template>
            <template #operation="{ row }"><ElButton v-if="row.eventStatus.startsWith('FAILED')" link type="primary" @click="retry(row)">重试</ElButton></template>
          </ArtTable>
        </ArtTablePanel>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.ops-page { display:flex; flex-direction:column; gap:12px; height:100%; min-height:0; }
.ops-page > .el-card:first-child { border:0; flex-shrink:0; }
.table-card { flex:1; min-height:0; overflow:hidden; }
.table-card :deep(.el-card__body) { display:flex; flex-direction:column; height:100%; min-height:0; padding:16px; }
</style>
