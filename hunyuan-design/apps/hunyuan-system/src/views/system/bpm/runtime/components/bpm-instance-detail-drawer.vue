<script setup lang="ts">
import type { BpmInstanceDetailRecord } from '#/api/system/bpm/runtime';

import { computed, ref } from 'vue';

import {
  getBpmAdminInstanceDetail,
  getBpmInstanceDetail,
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

defineOptions({ name: 'SystemBpmInstanceDetailDrawer' });

const visible = ref(false);
const loading = ref(false);
const detail = ref<BpmInstanceDetailRecord>();
const loadErrorMessage = ref('');
const currentTasks = computed(() => detail.value?.currentTasks ?? []);
const actionLogs = computed(() => detail.value?.actionLogs ?? []);
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
  loadErrorMessage.value = '';
  try {
    detail.value =
      source === 'admin'
        ? await getBpmAdminInstanceDetail(instanceId)
        : await getBpmInstanceDetail(instanceId);
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
