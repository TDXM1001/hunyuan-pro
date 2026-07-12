<script setup lang="ts">
import type { BpmExternalWaitRecord } from '#/api/system/bpm';
import type { ColumnOption } from '@vben/art-hooks/table';
import { computed, onMounted, reactive, ref } from 'vue';
import { ArtSearchPanel } from '@vben/art-hooks/common';
import { ArtTable, ArtTableHeader, ArtTablePanel, useTableColumns } from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';
import { ElButton, ElCard, ElFormItem, ElInput, ElMessage, ElMessageBox, ElOption, ElSelect, ElTag } from 'element-plus';
import { cancelBpmExternalWait, queryBpmExternalWaitPage, retryBpmExternalWait } from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmExternalWaitList' });
const loading = ref(false); const showSearchBar = ref(true); const rows = ref<BpmExternalWaitRecord[]>([]);
const searchForm = reactive({ connectorKey: '', instanceId: '', waitStatus: '' });
const pagination = reactive({ current: 1, size: 10, total: 0 });
const columnsFactory = (): ColumnOption<BpmExternalWaitRecord>[] => [
  { type:'globalIndex', label:'序号', width:70, align:'center' }, { prop:'correlationKey', label:'相关键', minWidth:190 },
  { prop:'connectorKey', label:'连接器', width:130 }, { prop:'operationKey', label:'操作', minWidth:140 },
  { prop:'instanceId', label:'实例ID', width:100 }, { prop:'nodeKey', label:'节点', minWidth:140 },
  { prop:'waitStatus', label:'状态', width:130, useSlot:true }, { prop:'timeoutAt', label:'超时时间', minWidth:175 },
  { prop:'lastError', label:'最后错误', minWidth:220 }, { prop:'operation', label:'操作', width:130, fixed:'right', useSlot:true },
];
const { columns, columnChecks } = useTableColumns(columnsFactory);
const tableHeight = computed(() => pagination.total > pagination.size ? 'calc(100% - 44px)' : '100%');
function statusType(status:string) { if(status==='RESUMED') return 'success'; if(status==='FAILED_MANUAL'||status==='TIMED_OUT') return 'danger'; if(status==='CANCELLED') return 'info'; return 'warning'; }
async function loadData(){ loading.value=true; try { const result=await queryBpmExternalWaitPage({ connectorKey:searchForm.connectorKey||undefined, instanceId:searchForm.instanceId?Number(searchForm.instanceId):undefined, waitStatus:searchForm.waitStatus||undefined, pageNum:pagination.current, pageSize:pagination.size }); rows.value=result?.list??[]; pagination.total=result?.total??0; } finally { loading.value=false; } }
function search(){ pagination.current=1; void loadData(); } function reset(){ Object.assign(searchForm,{connectorKey:'',instanceId:'',waitStatus:''}); search(); }
async function retry(row:BpmExternalWaitRecord){ await ElMessageBox.confirm('确认重新恢复该外部等待？','人工重试',{type:'warning'}); await retryBpmExternalWait(row.externalWaitId); ElMessage.success('等待已恢复'); await loadData(); }
async function cancel(row:BpmExternalWaitRecord){ await ElMessageBox.confirm('取消后该等待不再接受回调，确认继续？','取消等待',{type:'warning'}); await cancelBpmExternalWait(row.externalWaitId); ElMessage.success('等待已取消'); await loadData(); }
onMounted(() => void loadData());
</script>
<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden"><div class="ops-page">
    <ElCard v-show="showSearchBar" shadow="never"><ArtSearchPanel :collapsible="false" :loading="loading" :show-refresh="false" @reset="reset" @search="search">
      <ElFormItem label="实例ID"><ElInput v-model="searchForm.instanceId" clearable /></ElFormItem><ElFormItem label="连接器"><ElInput v-model="searchForm.connectorKey" clearable /></ElFormItem>
      <ElFormItem label="状态"><ElSelect v-model="searchForm.waitStatus" clearable><ElOption v-for="item in ['WAITING','RESUMED','TIMED_OUT','CANCELLED','FAILED_MANUAL']" :key="item" :label="item" :value="item" /></ElSelect></ElFormItem>
    </ArtSearchPanel></ElCard>
    <ElCard class="table-card" shadow="never"><ArtTablePanel><ArtTableHeader v-model="columnChecks" :loading="loading" layout="search,size,fullscreen,columns,settings" :show-search-bar="showSearchBar" @search="showSearchBar=!showSearchBar" />
      <ArtTable :columns="columns" :data="rows" :height="tableHeight" :loading="loading" :pagination="pagination" :pagination-options="{align:'center',hideOnSinglePage:false,layout:'sizes, prev, pager, next, jumper',pageSizes:[10,20,30],showTotalSummary:true,size:'small'}" @pagination:current-change="(v:number)=>{pagination.current=v;void loadData()}" @pagination:size-change="(v:number)=>{pagination.size=v;pagination.current=1;void loadData()}">
        <template #waitStatus="{row}"><ElTag :type="statusType(row.waitStatus)" effect="plain">{{row.waitStatus}}</ElTag></template>
        <template #operation="{row}"><ElButton v-if="row.waitStatus==='FAILED_MANUAL'" link type="primary" @click="retry(row)">重试</ElButton><ElButton v-if="row.waitStatus==='WAITING'" link type="danger" @click="cancel(row)">取消</ElButton></template>
      </ArtTable></ArtTablePanel></ElCard>
  </div></Page>
</template>
<style scoped>.ops-page{display:flex;flex-direction:column;gap:12px;height:100%;min-height:0}.ops-page>.el-card:first-child{border:0;flex-shrink:0}.table-card{flex:1;min-height:0;overflow:hidden}.table-card :deep(.el-card__body){display:flex;flex-direction:column;height:100%;min-height:0;padding:16px}</style>
