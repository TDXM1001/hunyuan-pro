<script setup lang="ts">
import type { BpmApprovalGroupDetailRecord } from '#/api/system/bpm';

import { computed } from 'vue';

import { ElEmpty, ElTable, ElTableColumn, ElTag } from 'element-plus';

defineOptions({ name: 'BpmApprovalGroupPanel' });

const props = withDefaults(
  defineProps<{
    group?: BpmApprovalGroupDetailRecord | null;
    groups?: BpmApprovalGroupDetailRecord[];
  }>(),
  {
    group: null,
    groups: () => [],
  },
);

const approvalGroups = computed(() =>
  props.group ? [props.group] : props.groups,
);

// 后端保留稳定枚举值，页面只负责转换为中文业务文案。
function getGroupStateLabel(value: string) {
  const labelMap: Record<string, string> = {
    APPROVED: '全员通过',
    CANCELLED: '已关闭',
    PENDING: '审批中',
    REJECTED: '已拒绝',
    RETURNED: '已退回',
  };
  return labelMap[value] ?? value;
}

function getGroupStateType(value: string) {
  if (value === 'APPROVED') return 'success';
  if (value === 'REJECTED') return 'danger';
  if (value === 'RETURNED') return 'warning';
  if (value === 'PENDING') return 'primary';
  return 'info';
}

function getApprovalModeLabel(mode: BpmApprovalGroupDetailRecord['approvalMode']) {
  return mode === 'sequential' ? '顺序审批' : '并行会签';
}

function getPendingActivationCount(group: BpmApprovalGroupDetailRecord) {
  if (group.approvalMode === 'sequential') {
    return Math.max(group.totalMemberCount - group.members.length, 0);
  }
  return 0;
}

function getCloseReasonLabel(value?: null | string) {
  const labelMap: Record<string, string> = {
    ALL_APPROVED: '全部成员已通过',
    INSTANCE_CANCELLED: '流程实例已取消',
    INSTANCE_RECALLED: '发起人已撤回',
    MEMBER_REJECTED: '有成员拒绝，审批组已终止',
    MEMBER_RETURNED: '有成员退回发起人，其他待办已取消',
  };
  return value ? (labelMap[value] ?? value) : '-';
}

function getTaskStateLabel(value?: null | number) {
  if (value === 1) return '待处理';
  if (value === 2) return '已完成';
  if (value === 3) return '已取消';
  return '未知';
}

function getTaskResultLabel(value?: null | number) {
  if (value === 1) return '通过';
  if (value === 2) return '拒绝';
  if (value === 3) return '退回';
  if (value === 4) return '实例取消';
  if (value === 5) return '已减签';
  if (value === 6) return '发起人撤回';
  return '-';
}

function getActionLabel(actionType?: null | string) {
  const labelMap: Record<string, string> = {
    ADD_SIGNED: '任务加签',
    APPROVED: '审批通过',
    APPROVAL_GROUP_ALL_APPROVED: '审批组全员通过',
    APPROVAL_GROUP_CANCELLED: '审批组已关闭',
    DELEGATED: '任务委派',
    PARALLEL_MEMBER_APPROVED: '会签成员通过',
    PARALLEL_MEMBER_REJECTED: '会签成员拒绝，审批组已终止',
    PARALLEL_MEMBER_RETURNED: '会签成员退回发起人，其他待办已取消',
    RECALLED: '发起人撤回',
    REJECTED: '审批拒绝',
    REDUCE_SIGNED: '任务减签',
    RETURNED_TO_INITIATOR: '退回发起人',
    TRANSFERRED: '任务转办',
  };
  return actionType ? (labelMap[actionType] ?? actionType) : '-';
}
</script>

<template>
  <div class="bpm-approval-group-panel">
    <div
      v-for="approvalGroup in approvalGroups"
      :key="approvalGroup.approvalGroupId"
      class="bpm-approval-group-panel__group"
    >
      <div class="bpm-approval-group-panel__header">
        <div>
          <strong>{{ approvalGroup.approvalGroupName }}</strong>
          <span>{{ getApprovalModeLabel(approvalGroup.approvalMode) }}</span>
          <span>
            {{ approvalGroup.processedMemberCount }}/{{ approvalGroup.totalMemberCount }} 已处理
          </span>
          <span v-if="getPendingActivationCount(approvalGroup) > 0">
            后续 {{ getPendingActivationCount(approvalGroup) }} 人待激活
          </span>
        </div>
        <ElTag
          :type="getGroupStateType(approvalGroup.groupState)"
          effect="plain"
          size="small"
        >
          {{ getGroupStateLabel(approvalGroup.groupState) }}
        </ElTag>
      </div>

      <div
        v-if="approvalGroup.groupState !== 'PENDING'"
        class="bpm-approval-group-panel__close"
      >
        <span>结束原因：{{ getCloseReasonLabel(approvalGroup.closeReason) }}</span>
        <span>结束时间：{{ approvalGroup.closedAt || '-' }}</span>
      </div>

      <ElTable :data="approvalGroup.members" border size="small">
        <ElTableColumn label="成员" min-width="130">
          <template #default="{ row }">
            <div class="bpm-approval-group-panel__member">
              <strong>{{ row.assigneeNameSnapshot || '-' }}</strong>
              <span>{{ row.assigneeDepartmentNameSnapshot || '-' }}</span>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="序号" width="70" align="center">
          <template #default="{ row }">
            {{ row.memberIndex || '-' }}/{{ row.memberTotal || approvalGroup.totalMemberCount }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="90">
          <template #default="{ row }">
            {{ getTaskStateLabel(row.taskState) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="结果" min-width="100">
          <template #default="{ row }">
            {{ getTaskResultLabel(row.taskResult) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="最后动作" min-width="150">
          <template #default="{ row }">
            {{ getActionLabel(row.lastAction?.actionType) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="意见" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.lastAction?.commentText || '-' }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="处理时间" min-width="165">
          <template #default="{ row }">
            {{ row.completedAt || row.cancelledAt || '-' }}
          </template>
        </ElTableColumn>
      </ElTable>
    </div>
    <ElEmpty v-if="approvalGroups.length === 0" description="暂无审批组" />
  </div>
</template>

<style scoped>
.bpm-approval-group-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bpm-approval-group-panel__group {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  overflow: hidden;
}

.bpm-approval-group-panel__header {
  align-items: center;
  background: var(--el-fill-color-light);
  display: flex;
  justify-content: space-between;
  min-height: 42px;
  padding: 8px 12px;
}

.bpm-approval-group-panel__header > div {
  align-items: baseline;
  display: flex;
  gap: 10px;
  min-width: 0;
}

.bpm-approval-group-panel__header span,
.bpm-approval-group-panel__close,
.bpm-approval-group-panel__member span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.bpm-approval-group-panel__close {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  padding: 8px 12px;
}

.bpm-approval-group-panel__member {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
</style>
