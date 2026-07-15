<script setup lang="ts">
import type { BpmPolicyType } from '#/api/system/bpm/policy';
import type { DictOption } from '#/api/system/dict';

import { onMounted, ref } from 'vue';

import { ElMessage, ElRadioButton, ElRadioGroup } from 'element-plus';

import { queryDictOptionsByCode } from '#/api/system/dict';

defineProps<{
  disabled?: boolean;
  modelValue: BpmPolicyType;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: BpmPolicyType];
}>();

const typeOptions = ref<DictOption[]>([]);

function change(value: boolean | number | string | undefined) {
  if (typeof value === 'string') {
    emit('update:modelValue', value as BpmPolicyType);
  }
}

async function loadDictOptions() {
  try {
    typeOptions.value = await queryDictOptionsByCode('BPM_POLICY_TYPE');
  } catch (error: any) {
    ElMessage.error(error?.message || '规则类型字典加载失败');
  }
}

onMounted(() => {
  void loadDictOptions();
});
</script>

<template>
  <ElRadioGroup
    :disabled="disabled"
    :model-value="modelValue"
    @update:model-value="change"
  >
    <ElRadioButton
      v-for="item in typeOptions"
      :key="item.value"
      :value="item.value"
    >
      {{ item.label }}
    </ElRadioButton>
  </ElRadioGroup>
</template>
