<script setup lang="ts">
import type {
  BpmApprovalSubjectContext,
  BpmTaskFormContext,
} from '#/api/system/bpm';

import { computed, ref, watch } from 'vue';

import {
  ElAlert,
  ElDescriptions,
  ElDescriptionsItem,
  ElEmpty,
  ElInput,
  ElTag,
} from 'element-plus';

import BpmRuntimeFormRenderer from './bpm-runtime-form-renderer.vue';

defineOptions({ name: 'BpmTaskFormWorkbench' });

const props = defineProps<{
  approvalSubjectContext?: BpmApprovalSubjectContext | null;
  formContext?: BpmTaskFormContext | null;
  readonly?: boolean;
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

function parseArray(jsonText?: null | string) {
  try {
    const parsed = JSON.parse(jsonText || '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value));
}

async function submitPatch() {
  if (props.approvalSubjectContext?.viewState === 'READY') {
    const editableFields = new Set(
      props.approvalSubjectContext.fieldPermissions
        .filter((permission) => permission.permission === 'EDITABLE')
        .map((permission) => permission.fieldKey),
    );
    const patch: Record<string, any> = {};
    editableFields.forEach((fieldKey) => {
      if (JSON.stringify(baseline.value[fieldKey]) !== JSON.stringify(formData.value[fieldKey])) {
        patch[fieldKey] = formData.value[fieldKey];
      }
    });
    return {
      formDataPatchJson: JSON.stringify(patch),
      formDataVersion: props.approvalSubjectContext.workingDataVersion ?? undefined,
      workingDataPatchJson: JSON.stringify(patch),
      workingDataVersion: props.approvalSubjectContext.workingDataVersion ?? undefined,
    };
  }
  if (!props.formContext) {
    return {
      formDataPatchJson: undefined,
      formDataVersion: undefined,
      workingDataPatchJson: undefined,
      workingDataVersion: undefined,
    };
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
    workingDataPatchJson: JSON.stringify(patch),
    workingDataVersion: props.formContext.dataVersion,
  };
}

const subjectFields = computed(() =>
  Object.entries(parseData(props.approvalSubjectContext?.fieldsJson || undefined)),
);
const lineItems = computed(() => parseArray(props.approvalSubjectContext?.lineItemsJson));
const attachments = computed(() => parseArray(props.approvalSubjectContext?.attachmentsJson));
const workingPermissions = computed(() =>
  (props.approvalSubjectContext?.fieldPermissions || []).filter((permission) =>
    Object.prototype.hasOwnProperty.call(formData.value, permission.fieldKey),
  ),
);

watch(
  () => [props.formContext, props.approvalSubjectContext] as const,
  ([formContext, approvalSubjectContext]) => {
    const value = approvalSubjectContext?.viewState === 'READY'
      ? parseData(approvalSubjectContext.workingDataJson || undefined)
      : parseData(formContext?.formDataJson);
    baseline.value = clone(value);
    formData.value = clone(value);
  },
  { immediate: true },
);

defineExpose({ submitPatch });
</script>

<template>
  <div class="bpm-task-form-workbench">
    <template v-if="approvalSubjectContext">
      <ElAlert
        v-if="approvalSubjectContext.viewState === 'DIAGNOSTIC_ERROR'"
        :closable="false"
        :title="approvalSubjectContext.diagnosticMessage || '审批对象加载失败'"
        type="error"
      />
      <template v-else>
        <div class="bpm-task-form-workbench__header">
          <div>
            <strong>{{ approvalSubjectContext.title || '审批对象' }}</strong>
            <div v-if="approvalSubjectContext.summary" class="bpm-task-form-workbench__summary">
              {{ approvalSubjectContext.summary }}
            </div>
          </div>
          <ElTag effect="plain" size="small">
            版本 {{ approvalSubjectContext.workingDataVersion }}
          </ElTag>
        </div>

        <ElDescriptions :column="2" border>
          <ElDescriptionsItem
            v-for="([key, value]) in subjectFields"
            :key="key"
            :label="key"
          >
            {{ value ?? '-' }}
          </ElDescriptionsItem>
        </ElDescriptions>

        <div v-if="lineItems.length > 0" class="bpm-task-form-workbench__section">
          <strong>明细</strong>
          <ElDescriptions
            v-for="(item, index) in lineItems"
            :key="index"
            :column="2"
            border
          >
            <ElDescriptionsItem
              v-for="(value, key) in item"
              :key="String(key)"
              :label="String(key)"
            >
              {{ value ?? '-' }}
            </ElDescriptionsItem>
          </ElDescriptions>
        </div>

        <div v-if="attachments.length > 0" class="bpm-task-form-workbench__section">
          <strong>附件</strong>
          <div v-for="(attachment, index) in attachments" :key="index">
            {{ attachment.fileName || attachment.fileKey || `附件 ${index + 1}` }}
          </div>
        </div>

        <div v-if="workingPermissions.length > 0" class="bpm-task-form-workbench__section">
          <strong>流程工作数据</strong>
          <ElDescriptions :column="1" border>
            <ElDescriptionsItem
              v-for="permission in workingPermissions"
              :key="permission.fieldKey"
              :label="permission.fieldKey"
            >
              <ElInput
                v-model="formData[permission.fieldKey]"
                :disabled="readonly || permission.permission !== 'EDITABLE'"
              />
            </ElDescriptionsItem>
          </ElDescriptions>
        </div>
      </template>
    </template>
    <template v-else>
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
    </template>
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

.bpm-task-form-workbench__section {
  display: grid;
  gap: 8px;
  margin-top: 16px;
}

.bpm-task-form-workbench__summary {
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>
