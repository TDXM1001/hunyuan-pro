<script setup lang="ts">
import type {
  BpmInstanceDetailRecord,
  BpmInstanceTraceRecord,
  BpmTaskActionLogRecord,
} from '#/api/system/bpm/runtime';

import { computed, ref } from 'vue';

import {
  getBpmAdminInstanceDetail,
  getBpmAdminInstanceTrace,
  getBpmInstanceDetail,
  getBpmInstanceTrace,
} from '#/api/system/bpm/runtime';

import {
  ElDescriptions,
  ElDescriptionsItem,
  ElDrawer,
  ElEmpty,
  ElMessage,
  ElSkeleton,
  ElTag,
  ElTimeline,
  ElTimelineItem,
} from 'element-plus';

import BpmApprovalGroupPanel from './bpm-approval-group-panel.vue';
import BpmApprovalStagePanel from './bpm-approval-stage-panel.vue';
import BpmRuntimeProcessGraph from './bpm-runtime-process-graph.vue';

defineOptions({ name: 'SystemBpmInstanceDetailDrawer' });

type DetailSource = 'admin' | 'runtime';

const visible = ref(false);
const loading = ref(false);
const detail = ref<BpmInstanceDetailRecord>();
const trace = ref<BpmInstanceTraceRecord>();
const loadErrorMessage = ref('');

const currentTasks = computed(() => detail.value?.currentTasks ?? []);
const actionLogs = computed(() => detail.value?.actionLogs ?? []);
const processGraph = computed(() => trace.value?.processGraph);
const approvalGroups = computed(
  () => trace.value?.approvalGroups ?? detail.value?.approvalGroups ?? [],
);
const approvalStages = computed(() => trace.value?.approvalStages ?? []);

function getActionLabel(actionType?: null | string) {
  const labelMap: Record<string, string> = {
    ADD_SIGNED: '任务加签',
    APPROVED: '审批通过',
    DELEGATED: '任务委派',
    INSTANCE_CANCELLED: '实例取消',
    M2_APPROVE: '审批通过',
    M2_MEMBER_INELIGIBLE: '成员失效',
    M2_MEMBER_TRANSFERRED: '成员转办',
    M2_REJECT: '审批拒绝',
    M2_RETURN: '退回发起人',
    PARALLEL_MEMBER_APPROVED: '会签成员通过',
    PARALLEL_MEMBER_REJECTED: '会签成员拒绝',
    PARALLEL_MEMBER_RETURNED: '会签成员退回发起人',
    RECALLED: '发起人撤回',
    REDUCE_SIGNED: '任务减签',
    REJECTED: '审批拒绝',
    RESUBMITTED: '重新提交',
    RETURNED_TO_INITIATOR: '退回发起人',
    TRANSFERRED: '转办',
  };
  return actionType ? (labelMap[actionType] ?? actionType) : '-';
}

function getCurrentNodeSummary(detailRecord?: BpmInstanceDetailRecord) {
  const json = detailRecord?.currentNodeSummaryJson?.trim();
  if (!json) {
    return '-';
  }
  try {
    const parsed = JSON.parse(json);
    if (!Array.isArray(parsed) || parsed.length === 0) {
      return '-';
    }
    const names = parsed
      .map((item) =>
        typeof item?.taskName === 'string' ? item.taskName.trim() : '',
      )
      .filter(Boolean);
    return names.length > 0 ? names.join('、') : '-';
  } catch {
    return '-';
  }
}

function businessActionLogs(logs: BpmTaskActionLogRecord[]) {
  return logs.filter((log) => log.actionType !== 'APPROVAL_GROUP_ALL_APPROVED');
}

async function open(instanceId: number, source: DetailSource = 'runtime') {
  visible.value = true;
  loading.value = true;
  detail.value = undefined;
  trace.value = undefined;
  loadErrorMessage.value = '';
  try {
    if (source === 'admin') {
      const [detailRecord, traceRecord] = await Promise.all([
        getBpmAdminInstanceDetail(instanceId),
        getBpmAdminInstanceTrace(instanceId),
      ]);
      detail.value = detailRecord;
      trace.value = traceRecord;
    } else {
      const [detailRecord, traceRecord] = await Promise.all([
        getBpmInstanceDetail(instanceId),
        getBpmInstanceTrace(instanceId),
      ]);
      detail.value = detailRecord;
      trace.value = traceRecord;
    }
  } catch (error: any) {
    loadErrorMessage.value = '流程详情加载失败，请稍后重试。';
    ElMessage.error(error?.message || loadErrorMessage.value);
  } finally {
    loading.value = false;
  }
}

defineExpose({ open });
</script>

<template>
  <ElDrawer v-model="visible" title="流程详情" size="min(640px, 100vw)">
    <ElSkeleton v-if="loading" animated />
    <div v-else-if="loadErrorMessage" class="bpm-instance-detail__error">
      <ElEmpty :description="loadErrorMessage" />
    </div>
    <div v-else-if="detail" class="bpm-instance-detail">
      <ElDescriptions :column="1" border>
        <ElDescriptionsItem label="流程编号">
          {{ detail.instanceNo }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="标题">
          {{ detail.title }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="发起人">
          {{ detail.startEmployeeNameSnapshot || '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="发起部门">
          {{ detail.startDepartmentNameSnapshot || '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="摘要">
          {{ detail.summary || '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="当前节点">
          {{ getCurrentNodeSummary(detail) }}
        </ElDescriptionsItem>
      </ElDescriptions>

      <div class="bpm-instance-detail__section-title">当前待办</div>
      <div v-if="currentTasks.length > 0" class="bpm-instance-detail__current-tasks">
        <div
          v-for="task in currentTasks"
          :key="task.taskId"
          class="bpm-instance-detail__current-task"
        >
          <strong>{{ task.taskName }}</strong>
          <span>{{ task.assigneeNameSnapshot || '-' }}</span>
          <span>{{ task.assignedAt || '-' }}</span>
        </div>
      </div>
      <ElEmpty v-else description="暂无当前待办" />

      <template v-if="approvalGroups.length > 0">
        <div class="bpm-instance-detail__section-title">审批组</div>
        <BpmApprovalGroupPanel :groups="approvalGroups" />
      </template>

      <template v-if="approvalStages.length > 0">
        <div class="bpm-instance-detail__section-title">审批阶段</div>
        <BpmApprovalStagePanel :stages="approvalStages" />
      </template>

      <div class="bpm-instance-detail__section-title">动作轨迹</div>
      <ElTimeline
        v-if="businessActionLogs(actionLogs).length > 0"
        class="bpm-instance-detail__timeline"
      >
        <ElTimelineItem
          v-for="log in businessActionLogs(actionLogs)"
          :key="log.actionLogId"
          :timestamp="log.actionAt || ''"
        >
          <div class="bpm-instance-detail__timeline-row">
            <strong>{{ log.actorNameSnapshot || '-' }}</strong>
            <ElTag effect="plain" size="small">
              {{ getActionLabel(log.actionType) }}
            </ElTag>
          </div>
          <p v-if="log.commentText" class="bpm-instance-detail__comment">
            {{ log.commentText }}
          </p>
        </ElTimelineItem>
      </ElTimeline>
      <ElEmpty v-else description="暂无动作轨迹" />

      <template v-if="trace && processGraph?.nodes?.length">
        <div class="bpm-instance-detail__section-title">流程路径</div>
        <BpmRuntimeProcessGraph :graph="processGraph" />
      </template>
    </div>
  </ElDrawer>
</template>

<style scoped>
.bpm-instance-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bpm-instance-detail__error {
  align-items: center;
  display: flex;
  justify-content: center;
  min-height: 320px;
}

.bpm-instance-detail__section-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
}

.bpm-instance-detail__current-tasks {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bpm-instance-detail__current-task {
  align-items: center;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(120px, 1fr) minmax(80px, 120px) minmax(140px, 180px);
  min-height: 36px;
  padding: 8px 10px;
}

.bpm-instance-detail__timeline {
  padding-top: 4px;
}

.bpm-instance-detail__timeline-row {
  align-items: center;
  display: inline-flex;
  gap: 8px;
}

.bpm-instance-detail__comment {
  color: var(--el-text-color-regular);
  line-height: 22px;
  margin: 6px 0 0;
}
</style>
