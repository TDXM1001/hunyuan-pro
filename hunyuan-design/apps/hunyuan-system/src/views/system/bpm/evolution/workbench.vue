<script setup lang="ts">
import type {
  BpmAffectedInstance,
  BpmMigrationBatchDetailVO,
  BpmMigrationItemVO,
  BpmMigrationOperationVO,
  GraphEvolutionDiffVO,
} from '#/api/system/bpm';

import { computed, reactive, ref } from 'vue';
import { Page } from '@vben/common-ui';
import {
  ElButton, ElDescriptions, ElDescriptionsItem, ElDialog, ElForm, ElFormItem,
  ElInput, ElInputNumber, ElMessage, ElMessageBox, ElSpace,
  ElStatistic, ElTable, ElTableColumn, ElTag,
} from 'element-plus';

import {
  executeBpmMigration, getBpmMigrationBatch, previewBpmMigration,
  queryAffectedInstances, queryGraphEvolutionDiff, disposeBpmMigrationItem,
} from '#/api/system/bpm';

const loading = ref(false);
const executing = ref(false);
const diff = ref<GraphEvolutionDiffVO>();
const affected = ref<BpmAffectedInstance[]>([]);
const selected = ref<BpmAffectedInstance[]>([]);
const batch = ref<BpmMigrationBatchDetailVO | BpmMigrationOperationVO>();
const auditVisible = ref(false);
const auditItem = ref<BpmMigrationItemVO>();
const form = reactive({
  auditBatchId: undefined as number | undefined,
  dataMappingJson: '{}',
  nodeMappingsJson: '{}',
  reason: '',
  sourceVersionId: undefined as number | undefined,
  targetVersionId: undefined as number | undefined,
});

const canPreview = computed(() => Boolean(
  diff.value?.semanticChanged && selected.value.length && form.reason.trim().length >= 6,
));
const canExecute = computed(() => Boolean(
  batch.value?.eligibleCount && batch.value.batchStatus === 'PREVIEWED',
));

function changeLabel(kind: string) {
  return ({
    DEPENDENCY_CHANGED: '依赖变化', EDGE_ADDED: '连接新增', EDGE_CONFIG_CHANGED: '连接变化',
    EDGE_REMOVED: '连接删除', LAYOUT_CHANGED: '布局变化', NODE_ADDED: '节点新增',
    NODE_CONFIG_CHANGED: '节点配置变化', NODE_REMOVED: '节点删除',
    SCOPE_ADDED: '作用域新增', SCOPE_CONFIG_CHANGED: '作用域变化', SCOPE_REMOVED: '作用域删除',
  } as Record<string, string>)[kind] || kind;
}

function itemStatusType(status: string) {
  if (status === 'ELIGIBLE' || status === 'SUCCEEDED') return 'success';
  if (status === 'BLOCKED') return 'warning';
  if (status === 'FAILED') return 'danger';
  return 'info';
}

function blockersLabel(json: string) {
  try {
    const blockers = JSON.parse(json || '[]') as Array<{ code: string; message: string }>;
    return blockers.map((item) => `${item.code}: ${item.message}`).join('；') || '-';
  } catch {
    return json || '-';
  }
}

async function handleAnalyze() {
  if (!form.sourceVersionId || !form.targetVersionId) {
    ElMessage.warning('请选择源版本和目标版本');
    return;
  }
  loading.value = true;
  batch.value = undefined;
  try {
    const [diffResult, instances] = await Promise.all([
      queryGraphEvolutionDiff(form.sourceVersionId, form.targetVersionId),
      queryAffectedInstances(form.sourceVersionId),
    ]);
    diff.value = diffResult;
    affected.value = instances ?? [];
    selected.value = [];
  } finally {
    loading.value = false;
  }
}

function parseObject(value: string, label: string) {
  const parsed = JSON.parse(value || '{}');
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') throw new Error(`${label}必须是 JSON 对象`);
  return parsed as Record<string, string>;
}

async function handlePreview() {
  if (!form.sourceVersionId || !form.targetVersionId || !canPreview.value) return;
  try {
    const nodeMappings = parseObject(form.nodeMappingsJson, '节点映射');
    parseObject(form.dataMappingJson, '数据映射');
    batch.value = await previewBpmMigration({
      dataMappingJson: form.dataMappingJson,
      idempotencyKey: `m8:${form.sourceVersionId}:${form.targetVersionId}:${Date.now()}`,
      instanceIds: selected.value.map((item) => item.instanceId),
      nodeMappings,
      reason: form.reason.trim(),
      sourceVersionId: form.sourceVersionId,
      targetVersionId: form.targetVersionId,
    });
    ElMessage.success('迁移预演已完成，请核对阻断原因和合格实例');
  } catch (error: any) {
    ElMessage.error(error?.message || '迁移预演失败');
  }
}

async function handleExecute() {
  if (!batch.value || !canExecute.value) return;
  await ElMessageBox.confirm(
    `将迁移 ${batch.value.eligibleCount} 个合格实例；阻断实例保持源版本。该操作不能物理回滚，是否确认？`,
    '确认迁移', { confirmButtonText: '确认迁移', type: 'warning' },
  );
  executing.value = true;
  try {
    batch.value = await executeBpmMigration(batch.value.migrationBatchId);
    ElMessage.success('迁移命令已完成，结果请以逐实例审计为准');
  } finally {
    executing.value = false;
  }
}

async function handleLoadAudit() {
  if (!form.auditBatchId) return;
  batch.value = await getBpmMigrationBatch(form.auditBatchId);
}

async function handleDisposition(row: unknown, action: 'COMPENSATED' | 'KEEP_SOURCE' | 'RETRY') {
  const item = row as BpmMigrationItemVO;
  const labels = { COMPENSATED: '登记补偿结果', KEEP_SOURCE: '确认保留源版本', RETRY: '复核后重试' };
  const { value } = await ElMessageBox.prompt(
    action === 'COMPENSATED' ? '请输入已核实的补偿结果' : '请输入处置原因和判断依据',
    labels[action], { confirmButtonText: '确认处置', inputPattern: /.{6,}/, inputErrorMessage: '至少输入 6 个字符', type: 'warning' },
  );
  batch.value = await disposeBpmMigrationItem(item.migrationItemId, {
    action,
    compensationResult: action === 'COMPENSATED' ? String(value) : undefined,
    reason: String(value),
  });
  ElMessage.success(`${labels[action]}已写入审计`);
}

function showAudit(item: unknown) {
  auditItem.value = item as BpmMigrationItemVO;
  auditVisible.value = true;
}

function hasAuditEvidence(item: unknown): item is BpmMigrationItemVO {
  return Object.prototype.hasOwnProperty.call(item, 'sourceSnapshotJson');
}
</script>

<template>
  <Page auto-content-height content-class="!p-3 h-full min-h-0 overflow-auto">
    <div class="evolution-page">
      <section class="evolution-page__controls">
        <div>
          <h2>迁移与演进</h2>
          <p>发布版本保持隔离；只有预演合格的静止实例可以进入确认迁移。</p>
        </div>
        <ElForm :inline="true" :model="form">
          <ElFormItem label="源版本">
            <ElInputNumber v-model="form.sourceVersionId" :controls="false" :min="1" placeholder="版本 ID" />
          </ElFormItem>
          <ElFormItem label="目标版本">
            <ElInputNumber v-model="form.targetVersionId" :controls="false" :min="1" placeholder="版本 ID" />
          </ElFormItem>
          <ElFormItem><ElButton :loading="loading" type="primary" @click="handleAnalyze">分析版本影响</ElButton></ElFormItem>
          <ElFormItem label="审计批次">
            <ElInputNumber v-model="form.auditBatchId" :controls="false" :min="1" placeholder="批次 ID" />
          </ElFormItem>
          <ElFormItem><ElButton @click="handleLoadAudit">加载迁移审计</ElButton></ElFormItem>
        </ElForm>
      </section>

      <section v-if="diff" class="evolution-page__diff">
        <div class="evolution-page__summary">
          <ElTag :type="diff.semanticChanged ? 'warning' : 'success'">语义变化：{{ diff.semanticChanged ? '是' : '否' }}</ElTag>
          <ElTag :type="diff.layoutChanged ? 'info' : 'success'">布局变化：{{ diff.layoutChanged ? '是' : '否' }}</ElTag>
          <span>受影响在途实例 {{ affected.length }} 个</span>
        </div>
        <div class="evolution-page__changes">
          <ElTag v-for="change in diff.changes" :key="`${change.kind}:${change.elementId || ''}`" effect="plain">
            {{ changeLabel(change.kind) }}{{ change.elementId ? ` · ${change.elementId}` : '' }}
          </ElTag>
        </div>
      </section>

      <section v-if="diff" class="evolution-page__body">
        <div class="evolution-page__instances">
          <div class="evolution-page__section-title">
            <strong>影响实例</strong><span>选择进入迁移预演的实例</span>
          </div>
          <ElTable :data="affected" height="310" row-key="instanceId" @selection-change="selected = $event">
            <ElTableColumn type="selection" width="48" />
            <ElTableColumn label="实例编号" min-width="160" prop="instanceNo" />
            <ElTableColumn label="标题" min-width="180" prop="title" show-overflow-tooltip />
            <ElTableColumn label="业务键" min-width="150" prop="businessKey" />
            <ElTableColumn label="活动任务" align="center" prop="activeTaskCount" width="90" />
            <ElTableColumn label="开始时间" min-width="170" prop="startedAt" />
          </ElTable>
        </div>
        <div class="evolution-page__mapping">
          <div class="evolution-page__section-title"><strong>映射与确认依据</strong><span>未填写的稳定 ID 按同名映射</span></div>
          <ElForm label-position="top">
            <ElFormItem label="节点映射 JSON"><ElInput v-model="form.nodeMappingsJson" :rows="4" type="textarea" /></ElFormItem>
            <ElFormItem label="数据映射 JSON"><ElInput v-model="form.dataMappingJson" :rows="3" type="textarea" /></ElFormItem>
            <ElFormItem label="迁移原因"><ElInput v-model="form.reason" :rows="3" maxlength="512" show-word-limit type="textarea" /></ElFormItem>
            <ElButton :disabled="!canPreview" type="primary" @click="handlePreview">迁移预演</ElButton>
          </ElForm>
        </div>
      </section>

      <section v-if="batch" class="evolution-page__result">
        <div class="evolution-page__section-title">
          <div><strong>批次 {{ batch.batchCode }}</strong><span>{{ batch.batchStatus }}</span></div>
          <ElButton :disabled="!canExecute" :loading="executing" type="danger" @click="handleExecute">确认迁移</ElButton>
        </div>
        <div class="evolution-page__metrics">
          <ElStatistic title="预演总数" :value="batch.totalCount" />
          <ElStatistic title="合格" :value="batch.eligibleCount" />
          <ElStatistic title="阻断" :value="batch.blockedCount" />
          <ElStatistic title="成功" :value="batch.succeededCount" />
          <ElStatistic title="失败" :value="batch.failedCount" />
        </div>
        <ElTable :data="batch.items" max-height="340" row-key="migrationItemId">
          <ElTableColumn label="实例 ID" prop="instanceId" width="100" />
          <ElTableColumn label="状态" width="110">
            <template #default="{ row }"><ElTag :type="itemStatusType(row.itemStatus)">{{ row.itemStatus }}</ElTag></template>
          </ElTableColumn>
          <ElTableColumn label="阻断原因" min-width="280" show-overflow-tooltip>
            <template #default="{ row }">{{ blockersLabel(row.blockersJson) }}</template>
          </ElTableColumn>
          <ElTableColumn label="失败/补偿结果" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">{{ row.failureReason || row.compensationResult || '-' }}</template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="280"><template #default="{ row }">
            <ElSpace>
              <ElButton v-if="hasAuditEvidence(row)" link type="primary" @click="showAudit(row)">迁移审计</ElButton>
              <template v-if="row.itemStatus === 'FAILED'">
                <ElButton link type="primary" @click="handleDisposition(row, 'RETRY')">复核重试</ElButton>
                <ElButton link type="warning" @click="handleDisposition(row, 'KEEP_SOURCE')">保留源版本</ElButton>
                <ElButton link type="danger" @click="handleDisposition(row, 'COMPENSATED')">登记补偿</ElButton>
              </template>
            </ElSpace>
          </template></ElTableColumn>
        </ElTable>
      </section>

      <ElDialog v-model="auditVisible" title="迁移审计" width="min(720px, calc(100vw - 24px))">
        <ElDescriptions v-if="auditItem" :column="2" border>
          <ElDescriptionsItem label="实例 ID">{{ auditItem.instanceId }}</ElDescriptionsItem>
          <ElDescriptionsItem label="结果">{{ auditItem.itemStatus }}</ElDescriptionsItem>
          <ElDescriptionsItem :span="2" label="阻断原因">{{ blockersLabel(auditItem.blockersJson) }}</ElDescriptionsItem>
          <ElDescriptionsItem :span="2" label="源快照"><pre>{{ auditItem.sourceSnapshotJson }}</pre></ElDescriptionsItem>
          <ElDescriptionsItem :span="2" label="目标快照"><pre>{{ auditItem.targetSnapshotJson || '-' }}</pre></ElDescriptionsItem>
          <ElDescriptionsItem :span="2" label="引擎证据"><pre>{{ auditItem.engineCommandEvidenceJson || '-' }}</pre></ElDescriptionsItem>
          <ElDescriptionsItem :span="2" label="补偿结果">{{ auditItem.compensationResult || '-' }}</ElDescriptionsItem>
          <ElDescriptionsItem label="执行人">{{ auditItem.executedByEmployeeId || '-' }}</ElDescriptionsItem>
          <ElDescriptionsItem label="处置人">{{ auditItem.dispositionByEmployeeId || '-' }}</ElDescriptionsItem>
        </ElDescriptions>
      </ElDialog>
    </div>
  </Page>
</template>

<style scoped>
.evolution-page { display: flex; flex-direction: column; gap: 14px; min-height: 100%; color: var(--el-text-color-primary); }
.evolution-page__controls { align-items: flex-end; display: flex; justify-content: space-between; gap: 20px; padding: 14px 16px; border-bottom: 1px solid var(--el-border-color-lighter); background: var(--el-bg-color); }
.evolution-page__controls h2 { margin: 0 0 4px; font-size: 20px; letter-spacing: 0; }
.evolution-page__controls p, .evolution-page__section-title span { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }
.evolution-page__controls :deep(.el-form-item) { margin-bottom: 0; }
.evolution-page__diff { display: grid; gap: 10px; padding: 0 16px; }
.evolution-page__summary, .evolution-page__changes { align-items: center; display: flex; flex-wrap: wrap; gap: 8px; }
.evolution-page__body { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(320px, .8fr); gap: 16px; padding: 0 16px; }
.evolution-page__instances, .evolution-page__mapping, .evolution-page__result { border: 1px solid var(--el-border-color-lighter); background: var(--el-bg-color); padding: 14px; }
.evolution-page__section-title { align-items: center; display: flex; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.evolution-page__section-title > div { display: flex; gap: 10px; align-items: baseline; }
.evolution-page__result { margin: 0 16px 16px; }
.evolution-page__metrics { display: grid; grid-template-columns: repeat(5, minmax(100px, 1fr)); gap: 12px; margin-bottom: 14px; padding: 10px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
pre { margin: 0; max-height: 160px; overflow: auto; white-space: pre-wrap; word-break: break-all; font-size: 12px; }
@media (max-width: 1080px) { .evolution-page__controls { align-items: flex-start; flex-direction: column; } .evolution-page__body { grid-template-columns: minmax(0, 1fr); } }
@media (max-width: 640px) { .evolution-page__controls, .evolution-page__body { padding-left: 10px; padding-right: 10px; } .evolution-page__metrics { grid-template-columns: repeat(2, 1fr); } .evolution-page__result { margin-left: 10px; margin-right: 10px; } }
</style>
