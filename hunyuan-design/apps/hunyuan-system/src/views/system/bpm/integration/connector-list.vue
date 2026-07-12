<script setup lang="ts">
import type { BpmConnectorDefinitionRecord } from '#/api/system/bpm';
import type { ColumnOption } from '@vben/art-hooks/table';
import { computed, onMounted, reactive, ref } from 'vue';
import { ArtSearchPanel } from '@vben/art-hooks/common';
import { ArtTable, ArtTableHeader, ArtTablePanel, useTableColumns } from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';
import { ElButton, ElCard, ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElOption, ElSelect, ElTag } from 'element-plus';
import { queryBpmConnectorPage, saveBpmConnector } from '#/api/system/bpm';

defineOptions({ name: 'SystemBpmIntegrationConnectorList' });
const loading=ref(false); const saving=ref(false); const showSearchBar=ref(true); const dialogVisible=ref(false);
const rows=ref<BpmConnectorDefinitionRecord[]>([]); const searchForm=reactive({connectorKey:'',connectorName:'',enabledState:''});
const pagination=reactive({current:1,size:10,total:0});
const emptyForm=():BpmConnectorDefinitionRecord=>({connectorKey:'',connectorVersion:1,connectorName:'',baseEndpointRef:'env:',credentialRef:'env:',allowedOperationsJson:'[{"operationKey":"invoke","path":"/api","method":"POST","idempotent":false}]',timeoutMillis:5000,retryPolicyJson:'{"maxAttempts":1}',circuitPolicyJson:'{}',requestSchemaJson:'{}',responseSchemaJson:'{}',enabledState:'DISABLED'});
const form=reactive<BpmConnectorDefinitionRecord>(emptyForm());
const columnsFactory=():ColumnOption<BpmConnectorDefinitionRecord>[]=>[
  {type:'globalIndex',label:'序号',width:70,align:'center'}, {prop:'connectorKey',label:'连接器编码',minWidth:150},
  {prop:'connectorVersion',label:'版本',width:80,align:'center'}, {prop:'connectorName',label:'名称',minWidth:150},
  {prop:'baseEndpointRef',label:'端点引用',minWidth:190}, {prop:'timeoutMillis',label:'超时(ms)',width:110},
  {prop:'enabledState',label:'状态',width:140,useSlot:true}, {prop:'operation',label:'操作',width:90,fixed:'right',useSlot:true},
];
const {columns,columnChecks}=useTableColumns(columnsFactory); const tableHeight=computed(()=>pagination.total>pagination.size?'calc(100% - 44px)':'100%');
async function loadData(){loading.value=true;try{const result=await queryBpmConnectorPage({connectorKey:searchForm.connectorKey||undefined,connectorName:searchForm.connectorName||undefined,enabledState:searchForm.enabledState||undefined,pageNum:pagination.current,pageSize:pagination.size});rows.value=result?.list??[];pagination.total=result?.total??0;}finally{loading.value=false;}}
function search(){pagination.current=1;void loadData()} function reset(){Object.assign(searchForm,{connectorKey:'',connectorName:'',enabledState:''});search()}
function openCreate(){Object.assign(form,emptyForm());dialogVisible.value=true} function openEdit(row:BpmConnectorDefinitionRecord){Object.assign(form,JSON.parse(JSON.stringify(row)));dialogVisible.value=true}
function validateJson(value:string,label:string){try{JSON.parse(value);return true}catch{ElMessage.error(`${label}不是合法 JSON`);return false}}
async function submit(){if(!form.connectorKey||!form.connectorName||!form.baseEndpointRef.startsWith('env:')){ElMessage.warning('请填写连接器编码、名称和 env: 端点引用');return}if(!validateJson(form.allowedOperationsJson,'允许操作')||!validateJson(form.retryPolicyJson,'重试策略')||!validateJson(form.requestSchemaJson,'请求 Schema')||!validateJson(form.responseSchemaJson,'响应 Schema'))return;saving.value=true;try{await saveBpmConnector({...form,credentialRef:form.credentialRef?.trim()||undefined});ElMessage.success('连接器已保存');dialogVisible.value=false;await loadData();}finally{saving.value=false;}}
onMounted(()=>void loadData());
</script>
<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-hidden"><div class="connector-page">
    <ElCard v-show="showSearchBar" shadow="never"><ArtSearchPanel :collapsible="false" :loading="loading" :show-refresh="false" @reset="reset" @search="search"><ElFormItem label="编码"><ElInput v-model="searchForm.connectorKey" clearable /></ElFormItem><ElFormItem label="名称"><ElInput v-model="searchForm.connectorName" clearable /></ElFormItem><ElFormItem label="状态"><ElSelect v-model="searchForm.enabledState" clearable><ElOption label="已启用" value="ENABLED"/><ElOption label="已停用" value="DISABLED"/><ElOption label="紧急停用" value="EMERGENCY_DISABLED"/></ElSelect></ElFormItem></ArtSearchPanel></ElCard>
    <ElCard class="table-card" shadow="never"><ArtTablePanel><ArtTableHeader v-model="columnChecks" :loading="loading" layout="search,size,fullscreen,columns,settings" :show-search-bar="showSearchBar" @search="showSearchBar=!showSearchBar"><template #left><ElButton type="primary" @click="openCreate">新增连接器</ElButton></template></ArtTableHeader><ArtTable :columns="columns" :data="rows" :height="tableHeight" :loading="loading" :pagination="pagination" :pagination-options="{align:'center',hideOnSinglePage:false,layout:'sizes, prev, pager, next, jumper',pageSizes:[10,20,30],showTotalSummary:true,size:'small'}" @pagination:current-change="(v:number)=>{pagination.current=v;void loadData()}" @pagination:size-change="(v:number)=>{pagination.size=v;pagination.current=1;void loadData()}"><template #enabledState="{row}"><ElTag :type="row.enabledState==='ENABLED'?'success':row.enabledState==='EMERGENCY_DISABLED'?'danger':'info'" effect="plain">{{row.enabledState}}</ElTag></template><template #operation="{row}"><ElButton link type="primary" @click="openEdit(row)">编辑</ElButton></template></ArtTable></ArtTablePanel></ElCard>
  </div>
  <ElDialog v-model="dialogVisible" title="连接器定义" width="min(760px, 96vw)" destroy-on-close><ElForm label-position="top" class="connector-form"><div class="form-grid"><ElFormItem label="连接器编码"><ElInput v-model="form.connectorKey" :disabled="!!form.connectorDefinitionId" /></ElFormItem><ElFormItem label="版本"><ElInputNumber v-model="form.connectorVersion" :min="1" /></ElFormItem><ElFormItem label="名称"><ElInput v-model="form.connectorName" /></ElFormItem><ElFormItem label="状态"><ElSelect v-model="form.enabledState"><ElOption label="已启用" value="ENABLED"/><ElOption label="已停用" value="DISABLED"/><ElOption label="紧急停用" value="EMERGENCY_DISABLED"/></ElSelect></ElFormItem><ElFormItem label="端点安全引用"><ElInput v-model="form.baseEndpointRef" placeholder="env:FINANCE_ENDPOINT" /></ElFormItem><ElFormItem label="凭据安全引用"><ElInput v-model="form.credentialRef" placeholder="env:FINANCE_TOKEN" /></ElFormItem><ElFormItem label="超时毫秒"><ElInputNumber v-model="form.timeoutMillis" :min="100" :max="60000" /></ElFormItem></div><ElFormItem label="允许操作 JSON"><ElInput v-model="form.allowedOperationsJson" type="textarea" :rows="4" /></ElFormItem><ElFormItem label="重试策略 JSON"><ElInput v-model="form.retryPolicyJson" type="textarea" :rows="2" /></ElFormItem><div class="form-grid"><ElFormItem label="请求 Schema JSON"><ElInput v-model="form.requestSchemaJson" type="textarea" :rows="4" /></ElFormItem><ElFormItem label="响应 Schema JSON"><ElInput v-model="form.responseSchemaJson" type="textarea" :rows="4" /></ElFormItem></div></ElForm><template #footer><ElButton @click="dialogVisible=false">取消</ElButton><ElButton :loading="saving" type="primary" @click="submit">保存</ElButton></template></ElDialog>
  </Page>
</template>
<style scoped>.connector-page{display:flex;flex-direction:column;gap:12px;height:100%;min-height:0}.connector-page>.el-card:first-child{border:0;flex-shrink:0}.table-card{flex:1;min-height:0;overflow:hidden}.table-card :deep(.el-card__body){display:flex;flex-direction:column;height:100%;min-height:0;padding:16px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 16px}.connector-form :deep(.el-select),.connector-form :deep(.el-input-number){width:100%}@media(max-width:640px){.form-grid{grid-template-columns:1fr}}</style>
