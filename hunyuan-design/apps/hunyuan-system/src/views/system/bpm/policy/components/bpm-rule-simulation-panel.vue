<script setup lang="ts">
import type {
  BpmPolicySimulationResult,
  BpmPolicyVisualDraft,
} from '#/api/system/bpm/policy';

import { computed, ref } from 'vue';

import { ElButton, ElEmpty, ElFormItem, ElMessage, ElTag } from 'element-plus';

import BpmIdentityPicker from '#/components/bpm/identity/bpm-identity-picker.vue';
import { simulateBpmPolicy } from '#/api/system/bpm/policy';

const props = defineProps<{ draft: BpmPolicyVisualDraft }>();

const starter = ref();
const result = ref<BpmPolicySimulationResult>();
const loading = ref(false);

const simulationError = computed(() => {
  const finding = result.value?.findings?.[0];
  if (!finding) {
    return '';
  }
  return finding.suggestion
    ? `${finding.message}；${finding.suggestion}`
    : finding.message;
});

async function run() {
  if (!starter.value) {
    return;
  }

  loading.value = true;
  try {
    result.value = await simulateBpmPolicy({
      draft: props.draft,
      starterEmployeeId: starter.value.stableId,
    });

    if (result.value.findings?.length) {
      ElMessage.warning(result.value.findings[0]?.message || '模拟未通过');
      return;
    }

    if (!result.value.resolvedMembers.length) {
      ElMessage.warning('没有解析到候选人');
      return;
    }

    ElMessage.success('模拟完成');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="simulation-panel">
    <ElFormItem
      class="art-edit-section__full"
      :error="simulationError || undefined"
      label="模拟发起人"
    >
      <div class="simulation-row">
        <BpmIdentityPicker v-model="starter" kind="EMPLOYEE" />
        <ElButton
          :disabled="!starter"
          :loading="loading"
          type="primary"
          @click="run"
        >
          运行模拟
        </ElButton>
      </div>
    </ElFormItem>

    <div
      v-if="result?.resolvedMembers.length"
      class="art-edit-section__full result"
    >
      <div class="members">
        <ElTag
          v-for="member in result.resolvedMembers"
          :key="member.employeeId"
          effect="plain"
        >
          {{ member.employeeName }}
          <template v-if="member.departmentName">
            · {{ member.departmentName }}
          </template>
        </ElTag>
      </div>
    </div>

    <div
      v-else-if="result && !result.findings.length"
      class="art-edit-section__full"
    >
      <ElEmpty description="没有候选人" />
    </div>
  </div>
</template>

<style scoped>
.simulation-panel {
  display: contents;
}

.simulation-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.simulation-row > :first-child {
  flex: 1;
  min-width: 220px;
}

.result {
  margin-top: 4px;
  padding-bottom: 14px;
}

.members {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
