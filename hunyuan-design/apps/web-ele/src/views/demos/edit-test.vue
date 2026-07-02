<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import type { ArtAttachmentItem } from '@vben/art-hooks/edit'
import type { ColumnOption } from '@vben/art-hooks/table'

import { computed, reactive, ref } from 'vue'

import {
  ArtAttachmentTable,
  ArtAttachmentUpload,
  ArtEditPage,
  ArtEditSection,
  ArtImageUpload,
} from '@vben/art-hooks/edit'
import { ArtPageActions, type ArtActionItem } from '@vben/art-hooks/common'
import { ArtTable } from '@vben/art-hooks/table'

import {
  ArrowLeft,
  Plus,
} from '@element-plus/icons-vue'
import {
  ElButton,
  ElDatePicker,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElRadio,
  ElRadioGroup,
  ElSelect,
  ElSwitch,
  ElTag,
} from 'element-plus'

interface MappingRule {
  id: number
  name: string
  sourceField: string
  targetField: string
  matchMode: string
  priority: number
  enabled: boolean
}

interface MasterDataForm {
  accessMode: string
  category: string
  code: string
  customAttributes: string
  description: string
  domain: string
  effectiveDate: string
  incrementFields: string
  lastSyncTime: string
  name: string
  owner: string
  partitionStrategy: string
  primaryFields: string
  provider: string
  qualityRequirement: string
  realtime: boolean
  remark: string
  sourceDatabase: string
  sourceName: string
  sourceOwner: string
  sourceSystem: string
  sourceTable: string
  sourceType: string
  status: string
  syncDescription: string
  syncFrequency: string
  syncMode: string
  syncPeriod: string[]
  syncStatus: string
  tags: string[]
  type: string
  usageInstruction: string
}

const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive<MasterDataForm>({
  accessMode: 'API',
  category: '客户域',
  code: 'CUST',
  customAttributes: '业务规则：客户编码唯一；手机号、证件号脱敏展示。',
  description: '用于统一客户身份、客户基础属性与跨系统客户编码映射。',
  domain: '营销主题域',
  effectiveDate: '2026-06-30',
  incrementFields: 'updated_at, version',
  lastSyncTime: '-',
  name: '客户',
  owner: '张三',
  partitionStrategy: '按自然月分区',
  primaryFields: 'cust_code',
  provider: '客户中台',
  qualityRequirement: '完整率 ≥ 99%，编码重复率 = 0。',
  realtime: false,
  remark: '当前接入 CRM、计费与客服系统，后续扩展会员中心。',
  sourceDatabase: 'crm_core',
  sourceName: 'CRM 客户主数据',
  sourceOwner: '李四',
  sourceSystem: 'CRM',
  sourceTable: 'customer_base',
  sourceType: '业务数据库',
  status: 'enabled',
  syncDescription: '同步前校验字段完整性与编码唯一性，失败后保留最近一次成功快照。',
  syncFrequency: '每日 02:00',
  syncMode: '增量同步',
  syncPeriod: ['2026-06-30', '2026-12-31'],
  syncStatus: 'notStarted',
  tags: ['客户识别', '统一编码'],
  type: '核心主数据',
  usageInstruction: '适用于客户画像、营销活动圈选、客服统一视图。',
})

const rules: FormRules<MasterDataForm> = {
  code: [{ message: '请输入主数据编码', required: true, trigger: 'blur' }],
  name: [{ message: '请输入主数据名称', required: true, trigger: 'blur' }],
  sourceName: [{ message: '请输入数据源名称', required: true, trigger: 'blur' }],
  sourceType: [{ message: '请选择数据源类型', required: true, trigger: 'change' }],
  syncFrequency: [{ message: '请选择同步频率', required: true, trigger: 'change' }],
  syncMode: [{ message: '请选择同步方式', required: true, trigger: 'change' }],
  type: [{ message: '请选择主数据类型', required: true, trigger: 'change' }],
}

const mappingRules = ref<MappingRule[]>([
  {
    id: 1,
    name: '客户编码匹配',
    sourceField: 'source.cust_code',
    targetField: 'mdm.cust_code',
    matchMode: '完全匹配',
    priority: 1,
    enabled: true,
  },
  {
    id: 2,
    name: '客户名称模糊匹配',
    sourceField: 'source.cust_name',
    targetField: 'mdm.cust_name',
    matchMode: '模糊匹配',
    priority: 2,
    enabled: true,
  },
])

const demoImageUrl = 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 240 240%22%3E%3Crect width=%22240%22 height=%22240%22 rx=%2218%22 fill=%22%23eef2ff%22/%3E%3Cpath d=%22M44 158l42-44 32 32 30-38 48 50v30H44z%22 fill=%22%236366f1%22 opacity=%22.82%22/%3E%3Ccircle cx=%2282%22 cy=%2278%22 r=%2220%22 fill=%22%23f59e0b%22/%3E%3C/svg%3E'

const imageAttachments = ref<ArtAttachmentItem[]>([
  {
    name: '客户资料封面',
    status: 'success',
    thumbnailUrl: demoImageUrl,
    uid: 'image-1',
    url: demoImageUrl,
  },
])

const tableAttachments = ref<ArtAttachmentItem[]>([
  {
    category: '口径文档',
    mimeType: 'application/pdf',
    name: '客户主数据口径说明.pdf',
    remark: '评审通过后作为上线依据',
    size: 1887437,
    status: 'success',
    uid: 'doc-1',
  },
  {
    category: '字段模板',
    mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    name: '字段映射模板.xlsx',
    remark: '用于来源字段与主数据字段对照',
    size: 880640,
    status: 'ready',
    uid: 'doc-2',
  },
])

const mappingColumns: ColumnOption<MappingRule>[] = [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'name', label: '规则名称', minWidth: 180 },
  { prop: 'sourceField', label: '源字段', minWidth: 180 },
  { prop: 'targetField', label: '目标字段', minWidth: 180 },
  { prop: 'matchMode', label: '匹配方式', width: 120, align: 'center' },
  { prop: 'priority', label: '优先级', width: 100, align: 'center' },
  { prop: 'enabled', label: '启用状态', width: 120, align: 'center', useSlot: true },
  { prop: 'operation', label: '操作', width: 140, fixed: 'right', useSlot: true },
]

const pageActions = computed<ArtActionItem[]>(() => [
  {
    key: 'reset',
    label: '重置',
    onClick: resetForm,
  },
  {
    key: 'draft',
    label: '保存草稿',
    onClick: saveDraft,
  },
  {
    key: 'save',
    label: '保存',
    loading: saving.value,
    onClick: saveForm,
    type: 'primary',
  },
])

function addRule() {
  mappingRules.value.push({
    id: Date.now(),
    name: '新增匹配规则',
    sourceField: 'source.field',
    targetField: 'mdm.field',
    matchMode: '完全匹配',
    priority: mappingRules.value.length + 1,
    enabled: true,
  })
}

function removeRule(row: MappingRule) {
  mappingRules.value = mappingRules.value.filter((item) => item.id !== row.id)
}

async function saveForm() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  await new Promise((resolve) => setTimeout(resolve, 400))
  saving.value = false
  ElMessage.success('主数据已保存')
}

async function saveDraft() {
  saving.value = true
  await new Promise((resolve) => setTimeout(resolve, 300))
  saving.value = false
  ElMessage.success('草稿已保存')
}

function resetForm() {
  formRef.value?.resetFields()
}
</script>

<template>
  <div class="absolute inset-0 box-border flex min-h-0 flex-col overflow-hidden p-4">
    <ArtEditPage title="编辑主数据">
      <template #back>
        <ElButton :icon="ArrowLeft" circle />
      </template>

      <template #extra>
        <ElTag type="warning" effect="light" round>草稿</ElTag>
      </template>

      <template #actions>
        <ArtPageActions :actions="pageActions" />
      </template>

      <ElForm
        ref="formRef"
        class="master-data-edit-form"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <ArtEditSection title="基础信息" :index="1">
          <ElFormItem label="主数据名称" prop="name">
            <ElInput v-model="form.name" placeholder="请输入主数据名称，如：客户" />
          </ElFormItem>
          <ElFormItem label="主数据编码" prop="code">
            <ElInput v-model="form.code" placeholder="请输入唯一编码，如：CUST" />
          </ElFormItem>
          <ElFormItem label="主数据类型" prop="type">
            <ElSelect v-model="form.type" placeholder="请选择主数据类型">
              <ElOption label="核心主数据" value="核心主数据" />
              <ElOption label="参考主数据" value="参考主数据" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="所属分类" prop="category">
            <ElSelect v-model="form.category" placeholder="请选择所属分类">
              <ElOption label="客户域" value="客户域" />
              <ElOption label="产品域" value="产品域" />
              <ElOption label="组织域" value="组织域" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="所属主题域" prop="domain">
            <ElSelect v-model="form.domain" placeholder="请选择主题域">
              <ElOption label="营销主题域" value="营销主题域" />
              <ElOption label="交易主题域" value="交易主题域" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="负责人" prop="owner">
            <ElSelect v-model="form.owner" filterable placeholder="请选择负责人">
              <ElOption label="张三" value="张三" />
              <ElOption label="李四" value="李四" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="状态" prop="status">
            <ElRadioGroup v-model="form.status">
              <ElRadio value="enabled">启用</ElRadio>
              <ElRadio value="disabled">停用</ElRadio>
            </ElRadioGroup>
          </ElFormItem>
          <ElFormItem label="标签" prop="tags">
            <ElSelect v-model="form.tags" multiple filterable placeholder="请选择或输入标签">
              <ElOption label="客户识别" value="客户识别" />
              <ElOption label="统一编码" value="统一编码" />
              <ElOption label="营销触达" value="营销触达" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem class="art-edit-section__full" label="描述说明" prop="description">
            <ElInput
              v-model="form.description"
              maxlength="500"
              placeholder="请输入主数据的业务描述、用途、适用范围等信息"
              show-word-limit
              type="textarea"
            />
          </ElFormItem>
          <ElFormItem class="art-edit-section__full" label="备注" prop="remark">
            <ElInput
              v-model="form.remark"
              maxlength="500"
              placeholder="请输入备注信息（选填）"
              show-word-limit
              type="textarea"
            />
          </ElFormItem>
        </ArtEditSection>

        <ArtEditSection title="数据源" :index="2">
          <ElFormItem label="数据源名称" prop="sourceName">
            <ElInput v-model="form.sourceName" placeholder="请输入数据源名称" />
          </ElFormItem>
          <ElFormItem label="数据源类型" prop="sourceType">
            <ElSelect v-model="form.sourceType" placeholder="请选择数据源类型">
              <ElOption label="业务数据库" value="业务数据库" />
              <ElOption label="文件" value="文件" />
              <ElOption label="消息" value="消息" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="来源系统" prop="sourceSystem">
            <ElSelect v-model="form.sourceSystem" placeholder="请选择来源系统">
              <ElOption label="CRM" value="CRM" />
              <ElOption label="计费系统" value="计费系统" />
              <ElOption label="客服系统" value="客服系统" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="来源库" prop="sourceDatabase">
            <ElInput v-model="form.sourceDatabase" placeholder="请输入来源库名称" />
          </ElFormItem>
          <ElFormItem label="来源表" prop="sourceTable">
            <ElInput v-model="form.sourceTable" placeholder="请输入来源表名称" />
          </ElFormItem>
          <ElFormItem label="数据源负责人" prop="sourceOwner">
            <ElSelect v-model="form.sourceOwner" filterable placeholder="请选择数据源负责人">
              <ElOption label="张三" value="张三" />
              <ElOption label="李四" value="李四" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="接入方式" prop="accessMode">
            <ElRadioGroup v-model="form.accessMode">
              <ElRadio value="API">API</ElRadio>
              <ElRadio value="数据库">数据库</ElRadio>
              <ElRadio value="文件">文件</ElRadio>
              <ElRadio value="消息">消息</ElRadio>
            </ElRadioGroup>
          </ElFormItem>
          <ElFormItem label="数据提供方" prop="provider">
            <ElInput v-model="form.provider" placeholder="请输入数据提供方" />
          </ElFormItem>
          <ElFormItem label="生效日期" prop="effectiveDate">
            <ElDatePicker
              v-model="form.effectiveDate"
              placeholder="请选择日期"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </ElFormItem>
        </ArtEditSection>

        <ArtEditSection title="来源同步" :index="3">
          <ElFormItem label="同步方式" prop="syncMode">
            <ElSelect v-model="form.syncMode" placeholder="请选择同步方式">
              <ElOption label="全量同步" value="全量同步" />
              <ElOption label="增量同步" value="增量同步" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="同步频率" prop="syncFrequency">
            <ElSelect v-model="form.syncFrequency" placeholder="请选择同步频率">
              <ElOption label="实时" value="实时" />
              <ElOption label="每日 02:00" value="每日 02:00" />
              <ElOption label="每周一 02:00" value="每周一 02:00" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="同步周期" prop="syncPeriod">
            <ElDatePicker
              v-model="form.syncPeriod"
              end-placeholder="结束日期"
              range-separator="~"
              start-placeholder="开始日期"
              type="daterange"
              value-format="YYYY-MM-DD"
            />
          </ElFormItem>
          <ElFormItem label="增量字段" prop="incrementFields">
            <ElInput v-model="form.incrementFields" placeholder="多个字段用逗号分隔" />
          </ElFormItem>
          <ElFormItem label="主键字段" prop="primaryFields">
            <ElInput v-model="form.primaryFields" placeholder="多个字段用逗号分隔" />
          </ElFormItem>
          <ElFormItem label="分区策略" prop="partitionStrategy">
            <ElSelect v-model="form.partitionStrategy" placeholder="请选择分区策略">
              <ElOption label="按自然月分区" value="按自然月分区" />
              <ElOption label="按自然日分区" value="按自然日分区" />
              <ElOption label="不分区" value="不分区" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="是否实时同步" prop="realtime">
            <ElSwitch v-model="form.realtime" />
          </ElFormItem>
          <ElFormItem label="同步状态" prop="syncStatus">
            <ElTag type="info" effect="light">未同步</ElTag>
          </ElFormItem>
          <ElFormItem label="最近同步时间" prop="lastSyncTime">
            <ElInput v-model="form.lastSyncTime" disabled />
          </ElFormItem>
          <ElFormItem class="art-edit-section__full" label="同步说明" prop="syncDescription">
            <ElInput
              v-model="form.syncDescription"
              maxlength="500"
              placeholder="请输入同步说明、注意事项等"
              show-word-limit
              type="textarea"
            />
          </ElFormItem>
          <ElFormItem class="art-edit-section__full" label="关联规则">
            <div class="rule-table-wrap">
              <div class="rule-table-toolbar">
                <ElButton :icon="Plus" type="primary" plain @click="addRule">
                  新增规则
                </ElButton>
              </div>
              <ArtTable
                :columns="mappingColumns"
                :data="mappingRules"
                :height="220"
                empty-text="暂无关联规则"
              >
                <template #enabled="{ row }">
                  <ElTag :type="row.enabled ? 'success' : 'info'" size="small" effect="light">
                    {{ row.enabled ? '启用' : '停用' }}
                  </ElTag>
                </template>

                <template #operation="{ row }">
                  <ElButton link type="danger" @click="removeRule(row)">删除</ElButton>
                </template>
              </ArtTable>
            </div>
          </ElFormItem>
        </ArtEditSection>

        <ArtEditSection title="扩展信息" :index="4">
          <ElFormItem class="art-edit-section__full" label="自定义属性" prop="customAttributes">
            <ElInput
              v-model="form.customAttributes"
              maxlength="500"
              placeholder="请输入业务规则、约束条件等"
              show-word-limit
              type="textarea"
            />
          </ElFormItem>
          <ElFormItem label="质量要求" prop="qualityRequirement">
            <ElSelect v-model="form.qualityRequirement" allow-create filterable placeholder="请选择质量要求">
              <ElOption label="完整率 ≥ 99%，编码重复率 = 0。" value="完整率 ≥ 99%，编码重复率 = 0。" />
              <ElOption label="关键字段不能为空" value="关键字段不能为空" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem class="art-edit-section__full" label="使用说明" prop="usageInstruction">
            <ElInput
              v-model="form.usageInstruction"
              maxlength="500"
              placeholder="请输入使用说明、应用场景等"
              show-word-limit
              type="textarea"
            />
          </ElFormItem>
        </ArtEditSection>

        <ArtEditSection title="附件信息" :index="5">
          <ElFormItem label="基础附件">
            <ArtAttachmentUpload text="上传评审材料" />
          </ElFormItem>
          <ElFormItem label="图片材料">
            <ArtImageUpload v-model="imageAttachments" :max="4" />
          </ElFormItem>
          <ElFormItem class="art-edit-section__full" label="附件清单">
            <ArtAttachmentTable v-model="tableAttachments" />
          </ElFormItem>
          <ElFormItem class="art-edit-section__full" label="备注补充">
            <ElInput
              maxlength="500"
              placeholder="请输入附件相关说明或备注（选填）"
              show-word-limit
              type="textarea"
            />
          </ElFormItem>
        </ArtEditSection>
      </ElForm>
    </ArtEditPage>
  </div>
</template>

<style scoped>
.master-data-edit-form {
  display: grid;
  gap: 12px;
}

.master-data-edit-form :deep(.el-select),
.master-data-edit-form :deep(.el-date-editor) {
  width: 100%;
}

.master-data-edit-form :deep(.el-textarea__inner) {
  min-height: 52px !important;
}

.rule-table-wrap {
  width: 100%;
}

.rule-table-toolbar {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 8px;
}
</style>
