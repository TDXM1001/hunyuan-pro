<script setup lang="ts">
import type { Api, FormRule, Options } from '@form-create/element-ui';
import type { Component } from 'vue';

import formCreate from '@form-create/element-ui';
import { computed, markRaw, ref, watch } from 'vue';

import { ElEmpty } from 'element-plus';

import { queryEmployeePage } from '#/api/system/organization';

import {
  hasEmployeeSelectRule,
  normalizeRuntimeFormRules,
} from './bpm-runtime-form-rules';

defineOptions({ name: 'BpmRuntimeFormRenderer' });

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    modelValue?: Record<string, any>;
    schemaJson?: string;
  }>(),
  {
    disabled: false,
    modelValue: () => ({}),
    schemaJson: '[]',
  },
);

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>];
}>();

const formApi = ref<Api>();
const formCreateComponent = markRaw(
  (((formCreate as any).default ?? formCreate) as Component),
);
const employeeOptions = ref<{ label: string; value: number }[]>([]);

const rawFormRules = computed<FormRule[]>(() => {
  const parsed = safeParseJson<unknown>(props.schemaJson, []);
  if (Array.isArray(parsed)) {
    return parsed as FormRule[];
  }
  if (parsed && typeof parsed === 'object' && Array.isArray((parsed as { fields?: unknown }).fields)) {
    return (parsed as { fields: FormRule[] }).fields;
  }
  return [];
});

const formRules = computed<FormRule[]>(() =>
  normalizeRuntimeFormRules(
    rawFormRules.value,
    employeeOptions.value,
    loadEmployeeOptions,
  ),
);

const hasRules = computed(() => formRules.value.length > 0);

const formOptions = computed<Options>(() => ({
  form: {
    labelPosition: 'top',
  },
  resetBtn: false,
  submitBtn: false,
}));

const runtimeModel = computed<Record<string, any>>({
  get: () => props.modelValue ?? {},
  set: (value) => {
    emit('update:modelValue', cloneJson(value ?? {}));
  },
});

function cloneJson<T>(value: T): T {
  if (value === null || value === undefined) {
    return {} as T;
  }
  return JSON.parse(JSON.stringify(value)) as T;
}

function safeParseJson<T>(jsonText: string | undefined, fallbackValue: T): T {
  if (!jsonText?.trim()) {
    return fallbackValue;
  }

  try {
    return JSON.parse(jsonText) as T;
  } catch {
    return fallbackValue;
  }
}

async function loadEmployeeOptions(keyword = '') {
  const result = await queryEmployeePage({
    disabledFlag: false,
    keyword,
    pageNum: 1,
    pageSize: 20,
  });
  employeeOptions.value = (result?.list ?? []).map((item) => ({
    label: `${item.actualName}（${item.departmentName || '未分配部门'}）`,
    value: item.employeeId,
  }));
}

async function submit() {
  if (!hasRules.value || !formApi.value) {
    return cloneJson(props.modelValue ?? {});
  }

  return new Promise<Record<string, any>>((resolve, reject) => {
    formApi.value?.submit(
      (formData) => {
        const nextValue = cloneJson(formData as Record<string, any>);
        emit('update:modelValue', nextValue);
        resolve(nextValue);
      },
      () => {
        reject(new Error('FORM_VALIDATION_FAILED'));
      },
    );
  });
}

watch(
  rawFormRules,
  (rules) => {
    if (hasEmployeeSelectRule(rules)) {
      void loadEmployeeOptions();
    }
  },
  { immediate: true },
);

defineExpose({ submit });
</script>

<template>
  <div class="bpm-runtime-form-renderer">
    <ElEmpty
      v-if="!hasRules"
      description="当前流程未配置运行表单字段，可直接填写标题和摘要后提交"
    />
    <component
      :is="formCreateComponent"
      v-else
      v-model="runtimeModel"
      v-model:api="formApi"
      :disabled="disabled"
      :option="formOptions"
      :rule="formRules"
    />
  </div>
</template>

<style scoped>
.bpm-runtime-form-renderer {
  min-height: 280px;
  width: 100%;
}

.bpm-runtime-form-renderer :deep(.fc-form) {
  width: 100%;
}
</style>
