<script setup lang="ts">
import type { BpmApprovalStageTraceRecord } from '#/api/system/bpm/runtime';

import { ElTable, ElTableColumn, ElTag } from 'element-plus';

defineOptions({ name: 'BpmApprovalStagePanel' });

defineProps<{
  stages: BpmApprovalStageTraceRecord[];
}>();

function stateLabel(value: string) {
  const labels: Record<string, string> = {
    ACTIVE: '审批中',
    APPROVED: '已通过',
    CANCELLED: '已取消',
    EXCEPTION_PENDING: '等待人工处置',
    REJECTED: '已拒绝',
    RETURNED: '已退回',
  };
  return labels[value] ?? value;
}

function stateType(value: string) {
  if (value === 'APPROVED') return 'success';
  if (value === 'REJECTED') return 'danger';
  if (value === 'RETURNED' || value === 'EXCEPTION_PENDING') return 'warning';
  if (value === 'ACTIVE') return 'primary';
  return 'info';
}

function completionModeLabel(value: string) {
  const labels: Record<string, string> = {
    ALL: '全员通过',
    ANY: '任一通过',
    RATIO: '按比例通过',
    SEQUENTIAL: '顺序审批',
    SINGLE: '单人审批',
  };
  return labels[value] ?? value;
}

function actionResultLabel(value?: null | string) {
  const labels: Record<string, string> = {
    APPROVED: '通过',
    CANCELLED: '已取消',
    REJECTED: '拒绝',
    RETURNED: '退回',
  };
  return value ? (labels[value] ?? value) : '-';
}

function changeReasonLabel(value?: null | string) {
  const labels: Record<string, string> = {
    APPROVE: '审批通过',
    REJECT: '审批拒绝',
    RETURN: '退回发起人',
    TRANSFER: '转办',
  };
  return value ? (labels[value] ?? value) : '-';
}

function memberStateLabel(value: string) {
  const labels: Record<string, string> = {
    ACTIVE: '待审批',
    APPROVED: '已通过',
    CANCELLED: '已取消',
    INELIGIBLE: '成员失效',
    PLANNED: '待激活',
    REJECTED: '已拒绝',
    RETURNED: '已退回',
    TERMINATED: '已终止',
  };
  return labels[value] ?? value;
}
</script>

<template>
  <div class="bpm-approval-stage-panel">
    <div
      v-for="(stage, index) in stages"
      :key="stage.approvalStageId"
      class="bpm-approval-stage-panel__stage"
    >
      <div class="bpm-approval-stage-panel__header">
        <div>
          <strong>审批阶段 {{ index + 1 }}</strong>
          <span>{{ completionModeLabel(stage.completionMode) }}</span>
          <span>{{ stage.approvedMemberCount }}/{{ stage.effectiveMemberCount }} 已通过</span>
          <span>阈值 {{ stage.requiredApprovalCount }}</span>
          <span v-if="stage.completionMode === 'RATIO'">比例 {{ stage.ratioPercent }}%</span>
        </div>
        <ElTag :type="stateType(stage.stageState)" effect="plain" size="small">
          {{ stateLabel(stage.stageState) }}
        </ElTag>
      </div>

      <div class="bpm-approval-stage-panel__meta">
        <span>已处理 {{ stage.processedMemberCount }}/{{ stage.effectiveMemberCount }}</span>
      </div>

      <ElTable :data="stage.members" border size="small">
        <ElTableColumn label="序号" width="68" align="center" prop="memberOrder" />
        <ElTableColumn label="原审批人" min-width="110">
          <template #default="{ row }">
            {{ row.sourceEmployeeNameSnapshot || '-' }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="当前处理人" min-width="110">
          <template #default="{ row }">
            {{ row.currentEmployeeNameSnapshot || '-' }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="状态" min-width="105">
          <template #default="{ row }">{{ memberStateLabel(row.memberState) }}</template>
        </ElTableColumn>
        <ElTableColumn label="结果" min-width="100">
          <template #default="{ row }">{{ actionResultLabel(row.actionResult) }}</template>
        </ElTableColumn>
        <ElTableColumn label="处理时间" min-width="165">
          <template #default="{ row }">
            {{ row.completedAt || row.cancelledAt || row.activatedAt || '-' }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="说明" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ changeReasonLabel(row.changeReason) }}</template>
        </ElTableColumn>
      </ElTable>
    </div>
  </div>
</template>

<style scoped>
.bpm-approval-stage-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bpm-approval-stage-panel__stage {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  overflow: hidden;
}

.bpm-approval-stage-panel__header {
  align-items: center;
  background: var(--el-fill-color-light);
  display: flex;
  gap: 12px;
  justify-content: space-between;
  min-height: 42px;
  padding: 8px 12px;
}

.bpm-approval-stage-panel__header > div,
.bpm-approval-stage-panel__meta {
  align-items: baseline;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  min-width: 0;
}

.bpm-approval-stage-panel__header span,
.bpm-approval-stage-panel__meta {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.bpm-approval-stage-panel__meta {
  padding: 8px 12px;
}
</style>
