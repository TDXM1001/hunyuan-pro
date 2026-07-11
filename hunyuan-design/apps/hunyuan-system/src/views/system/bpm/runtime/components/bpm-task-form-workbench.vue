<script setup lang="ts">
import type { BpmTaskFormContext } from '#/api/system/bpm';

import { ref, watch } from 'vue';

import { ElEmpty, ElTag } from 'element-plus';

import BpmRuntimeFormRenderer from './bpm-runtime-form-renderer.vue';

defineOptions({ name: 'BpmTaskFormWorkbench' });

const props = defineProps<{
  formContext?: BpmTaskFormContext | null;
}>();

interface RuntimeFormExpose {
  submit: () => Promise<Record<string, any>>;
}

const rendererRef = ref<RuntimeFormExpose>();
const baseline = ref<Record<string, any>>({});
const formData = ref<Record<string, any>>({});

function parseData(jsonText?: string) {
  try {
    const parsed = JSON.parse(jsonText || '{}');
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? parsed
      : {};
  } catch {
    return {};
  }
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value));
}

async function submitPatch() {
  if (!props.formContext) {
    return { formDataPatchJson: undefined, formDataVersion: undefined };
  }
  const submitted = (await rendererRef.value?.submit()) ?? formData.value;
  const editableFields = new Set(
    props.formContext.permissions
      .filter((permission) => permission.permission === 'EDITABLE')
      .map((permission) => permission.fieldKey),
  );
  const patch: Record<string, any> = {};
  editableFields.forEach((fieldKey) => {
    if (JSON.stringify(baseline.value[fieldKey]) !== JSON.stringify(submitted[fieldKey])) {
      patch[fieldKey] = submitted[fieldKey];
    }
  });
  return {
    formDataPatchJson: JSON.stringify(patch),
    formDataVersion: props.formContext.dataVersion,
  };
}

watch(
  () => props.formContext,
  (context) => {
    const value = parseData(context?.formDataJson);
    baseline.value = clone(value);
    formData.value = clone(value);
  },
  { immediate: true },
);

defineExpose({ submitPatch });
</script>

<template>
  <div class="bpm-task-form-workbench">
    <div v-if="formContext" class="bpm-task-form-workbench__header">
      <strong>审批数据</strong>
      <ElTag effect="plain" size="small">版本 {{ formContext.dataVersion }}</ElTag>
    </div>
    <BpmRuntimeFormRenderer
      v-if="formContext"
      ref="rendererRef"
      v-model="formData"
      :field-permissions="formContext.permissions"
      :schema-json="formContext.formSchemaJson"
    />
    <ElEmpty v-else description="当前任务没有可访问的审批表单" />
  </div>
</template>

<style scoped>
.bpm-task-form-workbench {
  min-width: 0;
}

.bpm-task-form-workbench__header {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}
</style>
