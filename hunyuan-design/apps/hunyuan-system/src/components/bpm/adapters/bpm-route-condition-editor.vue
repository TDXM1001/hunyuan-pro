<script setup lang="ts">
import type { BpmRouteConditionDraft } from './types';

import { computed } from 'vue';
import { ElFormItem, ElInput, ElInputNumber, ElOption, ElSelect } from 'element-plus';

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    fieldOptions: { field: string; label: string; type: string }[];
    modelValue?: BpmRouteConditionDraft;
  }>(),
  { disabled: false, modelValue: () => ({}) },
);
const emit = defineEmits<{ 'update:modelValue': [value: BpmRouteConditionDraft] }>();

const condition = computed({
  get: () => props.modelValue || {},
  set: (value) => emit('update:modelValue', value),
});
const selectedField = computed(() =>
  props.fieldOptions.find((item) => item.field === condition.value.fieldKey),
);
const numericField = computed(() =>
  /number|inputnumber|amount|integer/i.test(selectedField.value?.type || ''),
);
const operators = computed(() =>
  numericField.value
    ? ['EQ', 'NE', 'GT', 'GTE', 'LT', 'LTE', 'IN', 'NOT_IN']
    : ['EQ', 'NE', 'CONTAINS', 'NOT_CONTAINS', 'IN', 'NOT_IN', 'IS_EMPTY', 'NOT_EMPTY'],
);

function update(key: string, value: unknown) {
  condition.value = {
    ...condition.value,
    sourceType: 'FORM_FIELD',
    valueType: numericField.value ? 'NUMBER' : 'STRING',
    [key]: value,
  };
}
</script>

<template>
  <div class="route-condition-editor">
    <ElFormItem label="条件字段">
      <ElSelect
        :disabled="disabled"
        :model-value="String(condition.fieldKey || '')"
        placeholder="选择表单字段"
        @change="(value) => update('fieldKey', value)"
      >
        <ElOption
          v-for="field in fieldOptions"
          :key="field.field"
          :label="`${field.label} (${field.field})`"
          :value="field.field"
        />
      </ElSelect>
    </ElFormItem>
    <ElFormItem label="操作符">
      <ElSelect
        :disabled="disabled || !condition.fieldKey"
        :model-value="String(condition.operator || '')"
        @change="(value) => update('operator', value)"
      >
        <ElOption v-for="operator in operators" :key="operator" :label="operator" :value="operator" />
      </ElSelect>
    </ElFormItem>
    <ElFormItem
      v-if="!['IS_EMPTY', 'NOT_EMPTY'].includes(String(condition.operator || ''))"
      label="比较值"
    >
      <ElInputNumber
        v-if="numericField"
        :disabled="disabled"
        :model-value="Number(condition.compareValue || 0)"
        @change="(value) => update('compareValue', value)"
      />
      <ElInput
        v-else
        :disabled="disabled"
        :model-value="String(condition.compareValue || '')"
        @change="(value) => update('compareValue', value)"
      />
    </ElFormItem>
  </div>
</template>

<style scoped>
.route-condition-editor {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
}
.route-condition-editor :deep(.el-form-item) { margin-bottom: 0; }
@media (width <= 720px) { .route-condition-editor { grid-template-columns: 1fr; } }
</style>
