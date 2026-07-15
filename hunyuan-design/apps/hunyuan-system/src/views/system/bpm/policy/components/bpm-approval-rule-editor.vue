<script setup lang="ts">
import type { ApprovalPolicyVisualDocument } from '#/api/system/bpm/policy';
import type { DictOption } from '#/api/system/dict';

import { onMounted, ref } from 'vue';

import {
  ElCheckbox,
  ElCheckboxGroup,
  ElFormItem,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElSelect,
} from 'element-plus';

import { queryDictOptionsByCode } from '#/api/system/dict';

const model = defineModel<ApprovalPolicyVisualDocument>({ required: true });

const completionOptions = ref<DictOption[]>([]);
const rejectionOptions = ref<DictOption[]>([]);
const allowedActionOptions = ref<DictOption[]>([]);

async function loadDictOptions() {
  try {
    const [completion, rejection, allowedAction] = await Promise.all([
      queryDictOptionsByCode('BPM_APPROVAL_COMPLETION_MODE'),
      queryDictOptionsByCode('BPM_APPROVAL_REJECTION_RULE'),
      queryDictOptionsByCode('BPM_APPROVAL_ALLOWED_ACTION'),
    ]);
    completionOptions.value = completion;
    rejectionOptions.value = rejection;
    allowedActionOptions.value = allowedAction;
  } catch (error: any) {
    ElMessage.error(error?.message || '审批方式字典加载失败');
  }
}

onMounted(() => {
  void loadDictOptions();
});
</script>

<template>
  <div class="approval-rule-editor">
    <ElFormItem label="通过方式">
      <ElSelect
        v-model="model.completionMode"
        no-data-text="请先在字典中心维护 BPM_APPROVAL_COMPLETION_MODE"
        placeholder="请选择通过方式"
      >
        <ElOption
          v-for="item in completionOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </ElSelect>
    </ElFormItem>

    <ElFormItem v-if="model.completionMode === 'RATIO'" label="通过比例">
      <ElInputNumber v-model="model.ratioPercent" :max="100" :min="1" />
    </ElFormItem>

    <ElFormItem label="拒绝方式">
      <ElSelect
        v-model="model.rejectionRule"
        no-data-text="请先在字典中心维护 BPM_APPROVAL_REJECTION_RULE"
        placeholder="请选择拒绝方式"
      >
        <ElOption
          v-for="item in rejectionOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </ElSelect>
    </ElFormItem>

    <ElFormItem class="art-edit-section__full" label="允许动作">
      <ElCheckboxGroup v-model="model.allowedActions">
        <ElCheckbox
          v-for="item in allowedActionOptions"
          :key="item.value"
          :value="item.value"
        >
          {{ item.label }}
        </ElCheckbox>
      </ElCheckboxGroup>
    </ElFormItem>
  </div>
</template>

<style scoped>
.approval-rule-editor {
  display: contents;
}
</style>
