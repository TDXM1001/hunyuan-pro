<script setup lang="ts">
import type {
  BpmInstanceDetailRecord,
  BpmInstanceTraceRecord,
} from '#/api/system/bpm/runtime';

import { computed, ref } from 'vue';

import {
  getBpmAdminInstanceDetail,
  getBpmAdminInstanceTrace,
  getBpmInstanceDetail,
} from '#/api/system/bpm/runtime';

import {
  ElDescriptions,
  ElDescriptionsItem,
  ElDrawer,
  ElEmpty,
  ElMessage,
  ElSkeleton,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTimeline,
  ElTimelineItem,
} from 'element-plus';

defineOptions({ name: 'SystemBpmInstanceDetailDrawer' });

const visible = ref(false);
const loading = ref(false);
const detail = ref<BpmInstanceDetailRecord>();
const trace = ref<BpmInstanceTraceRecord>();
const loadErrorMessage = ref('');
const currentTasks = computed(() => detail.value?.currentTasks ?? []);
const actionLogs = computed(() => detail.value?.actionLogs ?? []);
const callbackRecords = computed(() => trace.value?.callbackRecords ?? []);
const commandRecords = computed(() => trace.value?.commandRecords ?? []);
const traceCurrentTasks = computed(() => trace.value?.currentTasks ?? []);
const traceActionLogs = computed(() => trace.value?.actionLogs ?? []);
type DetailSource = 'admin' | 'runtime';

function getActionLabel(actionType?: null | string) {
  const labelMap: Record<string, string> = {
    APPROVED: '审批通过',
    INSTANCE_CANCELLED: '实例取消',
    REJECTED: '审批拒绝',
    RESUBMITTED: '重新提交',
    RETURNED_TO_INITIATOR: '退回发起人',
    TRANSFERRED: '转办',
  };
  return actionType ? (labelMap[actionType] ?? actionType) : '-';
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
      detail.value = await getBpmInstanceDetail(instanceId);
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
  <ElDrawer v-model="visible" title="流程详情" size="640px">
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
          <code>{{ detail.currentNodeSummaryJson || '-' }}</code>
        </ElDescriptionsItem>
        <ElDescriptionsItem label="表单快照">
          <code>{{ detail.currentFormDataSnapshotJson || '-' }}</code>
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

      <div class="bpm-instance-detail__section-title">动作轨迹</div>
      <ElTimeline v-if="actionLogs.length > 0" class="bpm-instance-detail__timeline">
        <ElTimelineItem
          v-for="log in actionLogs"
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
          <p
            v-if="log.fromAssigneeEmployeeId || log.toAssigneeEmployeeId"
            class="bpm-instance-detail__comment"
          >
            {{ log.fromAssigneeEmployeeId || '-' }} -> {{ log.toAssigneeEmployeeId || '-' }}
          </p>
        </ElTimelineItem>
      </ElTimeline>
      <ElEmpty v-else description="暂无动作轨迹" />

      <template v-if="trace">
        <div class="bpm-instance-detail__section-title">可靠性追踪</div>
        <div class="bpm-instance-detail__trace-summary">
          <div class="bpm-instance-detail__trace-item">
            <span>当前任务</span>
            <strong>{{ traceCurrentTasks.length }}</strong>
          </div>
          <div class="bpm-instance-detail__trace-item">
            <span>动作轨迹</span>
            <strong>{{ traceActionLogs.length }}</strong>
          </div>
          <div class="bpm-instance-detail__trace-item">
            <span>回调记录</span>
            <strong>{{ callbackRecords.length }}</strong>
          </div>
          <div class="bpm-instance-detail__trace-item">
            <span>命令记录</span>
            <strong>{{ commandRecords.length }}</strong>
          </div>
        </div>

        <div class="bpm-instance-detail__sub-title">回调记录</div>
        <ElTable
          v-if="callbackRecords.length > 0"
          :data="callbackRecords"
          border
          size="small"
        >
          <ElTableColumn
            label="事件ID"
            min-width="150"
            prop="eventId"
            show-overflow-tooltip
          />
          <ElTableColumn label="业务类型" min-width="100" prop="businessType" />
          <ElTableColumn label="业务ID" min-width="90" prop="businessId" />
          <ElTableColumn label="状态" min-width="80" prop="callbackStatus" />
          <ElTableColumn label="重试" min-width="70" prop="retryCount" />
          <ElTableColumn
            label="失败原因"
            min-width="160"
            prop="failureReason"
            show-overflow-tooltip
          />
        </ElTable>
        <ElEmpty v-else description="暂无回调记录" />

        <div class="bpm-instance-detail__sub-title">命令记录</div>
        <ElTable
          v-if="commandRecords.length > 0"
          :data="commandRecords"
          border
          size="small"
        >
          <ElTableColumn
            label="命令键"
            min-width="180"
            prop="commandKey"
            show-overflow-tooltip
          />
          <ElTableColumn label="命令类型" min-width="90" prop="commandType" />
          <ElTableColumn label="业务类型" min-width="100" prop="businessType" />
          <ElTableColumn label="业务ID" min-width="90" prop="businessId" />
          <ElTableColumn label="状态" min-width="80" prop="commandStatus" />
          <ElTableColumn
            label="失败原因"
            min-width="160"
            prop="failureReason"
            show-overflow-tooltip
          />
        </ElTable>
        <ElEmpty v-else description="暂无命令记录" />
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

.bpm-instance-detail code {
  white-space: pre-wrap;
  word-break: break-all;
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

.bpm-instance-detail__trace-summary {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.bpm-instance-detail__trace-item {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 54px;
  min-width: 0;
  padding: 8px 10px;
}

.bpm-instance-detail__trace-item span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.bpm-instance-detail__trace-item strong {
  color: var(--el-text-color-primary);
  font-size: 18px;
  line-height: 24px;
}

.bpm-instance-detail__sub-title {
  color: var(--el-text-color-regular);
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
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
