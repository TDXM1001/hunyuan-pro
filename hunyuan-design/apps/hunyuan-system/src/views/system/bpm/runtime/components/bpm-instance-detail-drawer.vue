<script setup lang="ts">
import type { BpmInstanceDetailRecord } from '#/api/system/bpm/runtime';

import { ref } from 'vue';

import { getBpmInstanceDetail } from '#/api/system/bpm/runtime';

import {
  ElDescriptions,
  ElDescriptionsItem,
  ElDrawer,
  ElEmpty,
  ElSkeleton,
  ElTag,
  ElTimeline,
  ElTimelineItem,
} from 'element-plus';

defineOptions({ name: 'SystemBpmInstanceDetailDrawer' });

const visible = ref(false);
const loading = ref(false);
const detail = ref<BpmInstanceDetailRecord>();

function getActionLabel(actionType?: null | string) {
  const labelMap: Record<string, string> = {
    APPROVED: '审批通过',
    REJECTED: '审批拒绝',
    RETURNED_TO_INITIATOR: '退回发起人',
    TRANSFERRED: '转办',
  };
  return actionType ? (labelMap[actionType] ?? actionType) : '-';
}

async function open(instanceId: number) {
  visible.value = true;
  loading.value = true;
  try {
    detail.value = await getBpmInstanceDetail(instanceId);
  } finally {
    loading.value = false;
  }
}

defineExpose({ open });
</script>

<template>
  <ElDrawer v-model="visible" title="流程详情" size="640px">
    <ElSkeleton v-if="loading" animated />
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

      <div class="bpm-instance-detail__timeline-title">动作轨迹</div>
      <ElTimeline v-if="detail.actionLogs.length > 0" class="bpm-instance-detail__timeline">
        <ElTimelineItem
          v-for="log in detail.actionLogs"
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
    </div>
  </ElDrawer>
</template>

<style scoped>
.bpm-instance-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bpm-instance-detail code {
  white-space: pre-wrap;
  word-break: break-all;
}

.bpm-instance-detail__timeline-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
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
