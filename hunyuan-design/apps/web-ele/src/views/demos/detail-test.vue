<script setup lang="ts">
import type { DetailSection } from '@vben/art-hooks/detail'
import type { ArtAttachmentItem } from '@vben/art-hooks/edit'
import type { ColumnOption } from '@vben/art-hooks/table'

import { computed } from 'vue'

import { ArtPageActions, type ArtActionItem } from '@vben/art-hooks/common'
import { ArtDetail, ArtDetailPage } from '@vben/art-hooks/detail'
import { ArtAttachmentTable } from '@vben/art-hooks/edit'
import { ArtTable } from '@vben/art-hooks/table'

import {
  ArrowLeft,
  Edit,
} from '@element-plus/icons-vue'
import {
  ElButton,
  ElSpace,
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

interface MasterDataDetail {
  id: string
  code: string
  name: string
  type: string
  category: string
  domain: string
  owner: string
  status: 'disabled' | 'enabled'
  tags: string[]
  description: string
  remark: string
  sourceName: string
  sourceType: string
  sourceSystem: string
  sourceDatabase: string
  sourceTable: string
  sourceOwner: string
  accessMode: string
  provider: string
  effectiveDate: string
  syncMode: string
  syncFrequency: string
  syncPeriod: string
  incrementFields: string
  primaryFields: string
  partitionStrategy: string
  realtime: boolean
  syncStatus: 'failed' | 'notStarted' | 'running' | 'synced'
  lastSyncTime: string
  syncDescription: string
  customAttributes: string
  qualityRequirement: string
  usageInstruction: string
  attachments: ArtAttachmentItem[]
  documents: ArtAttachmentItem[]
}

const detailData: MasterDataDetail = {
  id: 'MDM-CUST-202606',
  code: 'CUST',
  name: '客户',
  type: '核心主数据',
  category: '客户域',
  domain: '营销主题域',
  owner: '张三',
  status: 'enabled',
  tags: ['客户识别', '统一编码', '营销触达'],
  description: '用于统一客户身份、客户基础属性与跨系统客户编码映射。',
  remark: '当前接入 CRM、计费与客服系统，后续扩展会员中心。',
  sourceName: 'CRM 客户主数据',
  sourceType: '业务数据库',
  sourceSystem: 'CRM',
  sourceDatabase: 'crm_core',
  sourceTable: 'customer_base',
  sourceOwner: '李四',
  accessMode: 'API',
  provider: '客户中台',
  effectiveDate: '2026-06-30',
  syncMode: '增量同步',
  syncFrequency: '每日 02:00',
  syncPeriod: '2026-06-30 ~ 2026-12-31',
  incrementFields: 'updated_at, version',
  primaryFields: 'cust_code',
  partitionStrategy: '按自然月分区',
  realtime: false,
  syncStatus: 'notStarted',
  lastSyncTime: '-',
  syncDescription: '同步前校验字段完整性与编码唯一性，失败后保留最近一次成功快照。',
  customAttributes: '业务规则：客户编码唯一；手机号、证件号脱敏展示。',
  qualityRequirement: '完整率 ≥ 99%，编码重复率 = 0。',
  usageInstruction: '适用于客户画像、营销活动圈选、客服统一视图。',
  documents: [
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
      status: 'success',
      uid: 'doc-2',
    },
  ],
  attachments: [
    {
      category: '评审记录',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      name: '评审纪要.docx',
      remark: '数据治理委员会评审记录',
      size: 430080,
      status: 'success',
      uid: 'attachment-1',
    },
  ],
}

const mappingRules: MappingRule[] = [
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
  {
    id: 3,
    name: '统一社会信用代码匹配',
    sourceField: 'source.credit_code',
    targetField: 'mdm.credit_code',
    matchMode: '完全匹配',
    priority: 3,
    enabled: true,
  },
]

const mappingColumns: ColumnOption<MappingRule>[] = [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'name', label: '规则名称', minWidth: 180 },
  { prop: 'sourceField', label: '源字段', minWidth: 180 },
  { prop: 'targetField', label: '目标字段', minWidth: 180 },
  { prop: 'matchMode', label: '匹配方式', width: 120, align: 'center' },
  { prop: 'priority', label: '优先级', width: 100, align: 'center' },
  { prop: 'enabled', label: '启用状态', width: 120, align: 'center', useSlot: true },
]

const masterStatusText = computed(() => detailData.status === 'enabled' ? '启用' : '停用')
const masterStatusType = computed(() => detailData.status === 'enabled' ? 'success' : 'info')

const pageActions: ArtActionItem[] = [
  {
    key: 'back',
    label: '返回',
  },
  {
    icon: Edit,
    key: 'edit',
    label: '编辑',
    type: 'primary',
  },
]

const sections = computed<DetailSection<MasterDataDetail>[]>(() => [
  {
    key: 'basic',
    title: '基础信息',
    items: [
      { label: '主数据名称', prop: 'name' },
      { label: '主数据编码', prop: 'code' },
      { label: '主数据类型', prop: 'type' },
      { label: '所属分类', prop: 'category' },
      { label: '所属主题域', prop: 'domain' },
      { label: '负责人', prop: 'owner' },
      { label: '状态', prop: 'status', useSlot: true },
      { label: '标签', prop: 'tags', span: 2, useSlot: true },
      { label: '描述说明', prop: 'description', span: 3 },
      { label: '备注', prop: 'remark', span: 3 },
    ],
  },
  {
    key: 'source',
    title: '数据源',
    items: [
      { label: '数据源名称', prop: 'sourceName' },
      { label: '数据源类型', prop: 'sourceType' },
      { label: '来源系统', prop: 'sourceSystem' },
      { label: '来源库', prop: 'sourceDatabase' },
      { label: '来源表', prop: 'sourceTable' },
      { label: '数据源负责人', prop: 'sourceOwner' },
      { label: '接入方式', prop: 'accessMode' },
      { label: '数据提供方', prop: 'provider' },
      { label: '生效日期', prop: 'effectiveDate' },
    ],
  },
  {
    key: 'sync',
    title: '来源同步',
    items: [
      { label: '同步方式', prop: 'syncMode' },
      { label: '同步频率', prop: 'syncFrequency' },
      { label: '同步周期', prop: 'syncPeriod' },
      { label: '增量字段', prop: 'incrementFields' },
      { label: '主键字段', prop: 'primaryFields' },
      { label: '分区策略', prop: 'partitionStrategy' },
      { label: '是否实时同步', prop: 'realtime', formatter: (value) => (value ? '是' : '否') },
      { label: '同步状态', prop: 'syncStatus', useSlot: true },
      { label: '最近同步时间', prop: 'lastSyncTime' },
      { label: '同步说明', prop: 'syncDescription', span: 3 },
      { label: '关联规则', prop: 'id', span: 3, slotName: 'mappingRules', useSlot: true },
    ],
  },
  {
    key: 'extend',
    title: '扩展信息',
    items: [
      { label: '自定义属性', prop: 'customAttributes', span: 2 },
      { label: '质量要求', prop: 'qualityRequirement' },
      { label: '使用说明', prop: 'usageInstruction', span: 3 },
    ],
  },
  {
    key: 'attachments',
    title: '附件信息',
    items: [
      { label: '文档上传', prop: 'documents', span: 1, useSlot: true },
      { label: '附件上传', prop: 'attachments', span: 1, useSlot: true },
      { label: '备注补充', value: '附件用于评审与上线交接，业务页面可替换为真实上传列表。', span: 3 },
    ],
  },
])

function getSyncStatusType(status: MasterDataDetail['syncStatus']) {
  const statusMap = {
    failed: 'danger',
    notStarted: 'info',
    running: 'warning',
    synced: 'success',
  } as const
  return statusMap[status]
}

function getSyncStatusText(status: MasterDataDetail['syncStatus']) {
  const statusMap = {
    failed: '同步失败',
    notStarted: '未同步',
    running: '同步中',
    synced: '已同步',
  }
  return statusMap[status]
}
</script>

<template>
  <div class="absolute inset-0 box-border flex min-h-0 flex-col overflow-hidden p-4">
    <ArtDetailPage title="主数据详情">
      <template #back>
        <ElButton :icon="ArrowLeft" circle />
      </template>

      <template #extra>
        <ElTag :type="masterStatusType" effect="light" round>{{ masterStatusText }}</ElTag>
      </template>

      <template #actions>
        <ArtPageActions :actions="pageActions" />
      </template>

      <ArtDetail :data="detailData" :sections="sections" :columns="3" :label-width="112">
        <template #status="{ value }">
          <ElTag :type="value === 'enabled' ? 'success' : 'info'" effect="light">
            {{ value === 'enabled' ? '启用' : '停用' }}
          </ElTag>
        </template>

        <template #tags="{ value }">
          <ElSpace wrap size="small">
            <ElTag v-for="tag in value" :key="tag" effect="light" round>
              {{ tag }}
            </ElTag>
          </ElSpace>
        </template>

        <template #syncStatus="{ value }">
          <ElTag :type="getSyncStatusType(value)" effect="light">
            {{ getSyncStatusText(value) }}
          </ElTag>
        </template>

        <template #mappingRules>
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
          </ArtTable>
        </template>

        <template #documents="{ value }">
          <ArtAttachmentTable :model-value="value" readonly :height="180" />
        </template>

        <template #attachments="{ value }">
          <ArtAttachmentTable :model-value="value" readonly :height="180" />
        </template>
      </ArtDetail>
    </ArtDetailPage>
  </div>
</template>
